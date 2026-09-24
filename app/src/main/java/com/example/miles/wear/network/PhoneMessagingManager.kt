package com.example.miles.wear.network

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.example.miles.wear.data.model.PhoneConnectionStatus
import com.example.miles.wear.data.model.PhoneMirroredMetrics
import com.example.miles.wear.data.repository.MilesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Phone connection over the local network — **no Google Play Services**.
 *
 * Discovery + messaging run through [LanTransport] (UDP beacons + TCP JSON
 * frames on the same paths the old data-layer messages used), so:
 *  - "Phone Connected" means a real MILES phone app on the same WiFi
 *  - mirrored workouts, HR/cadence streaming and the offline queue all work
 * The public API is unchanged, so every screen keeps working untouched.
 */
class PhoneMessagingManager(
    private val context: Context,
    private val repository: MilesRepository
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _connectionStatus = MutableStateFlow(PhoneConnectionStatus())
    val connectionStatus: StateFlow<PhoneConnectionStatus> = _connectionStatus.asStateFlow()

    private val _mirroredMetrics = MutableStateFlow(PhoneMirroredMetrics())
    val mirroredMetrics: StateFlow<PhoneMirroredMetrics> = _mirroredMetrics.asStateFlow()

    private val _controlActions = MutableSharedFlow<Pair<String, String>>() // action, type
    val controlActions: SharedFlow<Pair<String, String>> = _controlActions.asSharedFlow()

    private var transport: LanTransport? = null

    companion object {
        // Kept as aliases so nothing else has to change
        const val PATH_HR_STREAM = LanProtocol.PATH_HR_STREAM
        const val PATH_CADENCE_STREAM = LanProtocol.PATH_CADENCE_STREAM
        const val PATH_WORKOUT_CONTROL = LanProtocol.PATH_WORKOUT_CONTROL
        const val PATH_PHONE_METRICS = LanProtocol.PATH_PHONE_METRICS
        const val PATH_FLUSH_QUEUE = LanProtocol.PATH_FLUSH_QUEUE
    }

    fun initialize() {
        if (transport != null) return
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val appVersion = runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"

        val lan = LanTransport(
            role = LanProtocol.ROLE_WEAR,
            selfId = "wear-$androidId",
            selfName = (Build.MANUFACTURER + " " + Build.MODEL).trim(),
            selfModel = Build.MODEL ?: "Wear OS",
            appVersion = appVersion,
            scope = scope,
            batteryProvider = { currentBatteryPercent() },
            statusProvider = {
                runCatching {
                    com.example.miles.wear.service.WorkoutTrackingService.isWorkoutActive
                }.getOrDefault(false).toString()
            },
            onMessage = { path, payload -> handleMessage(path, payload) },
            onPeersChanged = { publishStatus() }
        )
        transport = lan
        lan.start()
        publishStatus()
    }

    fun cleanup() {
        transport?.stop()
        transport = null
    }

    /** Real battery percent of this watch (or -1 when unavailable). */
    private fun currentBatteryPercent(): Int = runCatching {
        val intent = context.registerReceiver(null, android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }.getOrDefault(-1)

    // ---------- status ----------

    private fun publishStatus() {
        val lan = transport
        val phonePeers = lan?.peersNow()?.filter { it.role == LanProtocol.ROLE_PHONE }.orEmpty()
        val phone = phonePeers.maxByOrNull { it.name }
        _connectionStatus.value = PhoneConnectionStatus(
            isConnected = phone != null,
            phoneNodeName = phone?.name ?: "",
            phoneNodeId = phone?.id ?: "",
            // MILES phone app detection — local (package) + nearby (LAN peer)
            localAppInstalled = PhoneAppDetector.localAppInstalled(context),
            localAppVersion = PhoneAppDetector.localAppVersion(context) ?: "",
            nearbyPeerCount = phonePeers.size
        )
        if (phone != null) {
            // Auto flush offline buffered queue when the phone shows up
            scope.launch { flushOfflineQueue() }
        }
    }

    /** Kept name for API compatibility — now refreshes LAN peers. */
    fun refreshConnectedNodes() {
        publishStatus()
        Log.i(
            LanTransport.TAG,
            "scan → lanPeers=${transport?.peersNow()?.size ?: 0} " +
                "phone=${_connectionStatus.value.phoneNodeName.ifBlank { "none" }} " +
                "localInstalled=${_connectionStatus.value.localAppInstalled} " +
                "localVersion=${_connectionStatus.value.localAppVersion}"
        )
    }

    // ---------- inbound ----------

    private fun handleMessage(path: String, payload: JSONObject) {
        when (path) {
            LanProtocol.PATH_PHONE_METRICS -> {
                _mirroredMetrics.value = PhoneMirroredMetrics(
                    seconds = payload.optLong("sec", 0L),
                    meters = payload.optDouble("m", 0.0),
                    calories = payload.optInt("cal", 0),
                    heartRate = payload.optInt("hr", 0),
                    isPhoneActive = true
                )
            }
            LanProtocol.PATH_WORKOUT_CONTROL -> {
                val action = payload.optString("action", "")
                val type = payload.optString("workoutType", "Run")
                scope.launch { _controlActions.emit(Pair(action, type)) }
            }
            LanProtocol.PATH_PING -> sendToPhone(LanProtocol.PATH_PONG, JSONObject().toString())
            LanProtocol.PATH_SYNC_REQUEST -> scope.launch { flushOfflineQueue() }
            LanProtocol.PATH_WATCH_SETTINGS -> Log.i(
                LanTransport.TAG,
                "watch settings from phone: ${payload}"
            )
        }
    }

    // ---------- outbound ----------

    fun broadcastHeartRate(bpm: Int, accuracy: Int) {
        val payload = JSONObject().apply {
            put("bpm", bpm)
            put("accuracy", accuracy)
            put("timestamp", System.currentTimeMillis())
        }
        sendOrEnqueue(LanProtocol.PATH_HR_STREAM, "HR", payload.toString())
    }

    fun broadcastCadence(cadence: Int) {
        val payload = JSONObject().apply {
            put("cadence", cadence)
            put("timestamp", System.currentTimeMillis())
        }
        sendOrEnqueue(LanProtocol.PATH_CADENCE_STREAM, "CADENCE", payload.toString())
    }

    fun sendWorkoutControl(action: String, workoutType: String) {
        val payload = JSONObject().apply {
            put("action", action)
            put("workoutType", workoutType)
            put("source", "watch")
            put("timestamp", System.currentTimeMillis())
        }
        sendOrEnqueue(LanProtocol.PATH_WORKOUT_CONTROL, "CONTROL", payload.toString())
    }

    private fun sendToPhone(path: String, payloadJson: String) {
        scope.launch {
            val ok = transport?.send(LanProtocol.ROLE_PHONE, path, payloadJson) == true
            if (!ok) repository.enqueueTelemetry("CONTROL", path, payloadJson)
        }
    }

    private fun sendOrEnqueue(path: String, type: String, payload: String) {
        scope.launch {
            val sent = transport?.send(LanProtocol.ROLE_PHONE, path, payload) == true
            if (!sent) {
                // No phone on the LAN right now: keep it in Room for later
                repository.enqueueTelemetry(type, path, payload)
            }
        }
    }

    suspend fun flushOfflineQueue() {
        val pending = repository.getPendingBatch(50)
        if (pending.isEmpty()) return

        val batchArray = JSONArray()
        val idsToMark = mutableListOf<Long>()
        pending.forEach { item ->
            batchArray.put(JSONObject().apply {
                put("id", item.id)
                put("type", item.type)
                put("path", item.path)
                put("payload", item.payloadJson)
                put("timestamp", item.timestamp)
            })
            idsToMark.add(item.id)
        }
        val batchPayload = JSONObject().apply {
            put("items", batchArray)
            put("count", batchArray.length())
        }.toString()

        val sent = transport?.send(LanProtocol.ROLE_PHONE, LanProtocol.PATH_FLUSH_QUEUE, batchPayload) == true
        if (sent) {
            repository.markBatchSynced(idsToMark)
            Log.i(LanTransport.TAG, "flushed ${idsToMark.size} queued items to phone over LAN")
        } else {
            Log.i(LanTransport.TAG, "phone not reachable yet — ${pending.size} items stay queued")
        }
    }
}