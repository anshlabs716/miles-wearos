package com.example.miles.wear.network

import android.content.Context
import android.util.Log
import com.example.miles.wear.data.model.PhoneConnectionStatus
import com.example.miles.wear.data.model.PhoneMirroredMetrics
import com.example.miles.wear.data.repository.MilesRepository
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
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
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject

class PhoneMessagingManager(
    private val context: Context,
    private val repository: MilesRepository
) : MessageClient.OnMessageReceivedListener, CapabilityClient.OnCapabilityChangedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val messageClient: MessageClient = Wearable.getMessageClient(context)
    private val capabilityClient: CapabilityClient = Wearable.getCapabilityClient(context)
    private val nodeClient = Wearable.getNodeClient(context)

    companion object {
        const val CAPABILITY_WEAR_TRACKER = "miles_wear_tracker"
        const val CAPABILITY_PHONE_APP = "miles_phone_app"
        const val PATH_HR_STREAM = "/miles/sensors/hr"
        const val PATH_CADENCE_STREAM = "/miles/sensors/cadence"
        const val PATH_WORKOUT_CONTROL = "/miles/workout/control"
        const val PATH_PHONE_METRICS = "/miles/metrics/live"
        const val PATH_FLUSH_QUEUE = "/miles/queue/flush"
    }

    private val _connectionStatus = MutableStateFlow(PhoneConnectionStatus())
    val connectionStatus: StateFlow<PhoneConnectionStatus> = _connectionStatus.asStateFlow()

    private val _mirroredMetrics = MutableStateFlow(PhoneMirroredMetrics())
    val mirroredMetrics: StateFlow<PhoneMirroredMetrics> = _mirroredMetrics.asStateFlow()

    private val _controlActions = MutableSharedFlow<Pair<String, String>>() // action, type
    val controlActions: SharedFlow<Pair<String, String>> = _controlActions.asSharedFlow()

    private var targetPhoneNode: Node? = null

    fun initialize() {
        messageClient.addListener(this)
        capabilityClient.addListener(this, CAPABILITY_WEAR_TRACKER)

        scope.launch {
            try {
                // Advertise capability so phone app detects watch
                capabilityClient.addLocalCapability(CAPABILITY_WEAR_TRACKER).await()
                refreshConnectedNodes()
            } catch (e: Exception) {
                Log.e("PhoneMessagingManager", "Error initializing capability", e)
            }
        }
    }

    fun cleanup() {
        messageClient.removeListener(this)
        capabilityClient.removeListener(this)
    }

    suspend fun refreshConnectedNodes() {
        try {
            val nodes = nodeClient.connectedNodes.await()
            val phoneNode = nodes.firstOrNull() // nearest phone/watch node as messaging target
            targetPhoneNode = phoneNode

            // Do any reachable nodes run the MILES phone app (miles_phone_app capability)?
            var capabilityCount = 0
            try {
                val phoneCap = capabilityClient
                    .getCapability(CAPABILITY_PHONE_APP, CapabilityClient.FILTER_REACHABLE)
                    .await()
                capabilityCount = phoneCap.nodes.size
            } catch (_: Exception) {
                // capability lookup not available — node-only detection still works
            }

            _connectionStatus.value = PhoneConnectionStatus(
                isConnected = phoneNode != null,
                phoneNodeName = phoneNode?.displayName ?: "",
                phoneNodeId = phoneNode?.id ?: "",
                // MILES phone app detection — local + nearby
                localAppInstalled = PhoneAppDetector.localAppInstalled(context),
                localAppVersion = PhoneAppDetector.localAppVersion(context) ?: "",
                nearbyCapabilityCount = capabilityCount
            )

            if (phoneNode != null) {
                // Auto flush offline buffered queue
                flushOfflineQueue()
            }
        } catch (e: Exception) {
            Log.e("PhoneMessagingManager", "Error checking nodes", e)
            _connectionStatus.value = PhoneConnectionStatus(
                isConnected = false,
                localAppInstalled = PhoneAppDetector.localAppInstalled(context),
                localAppVersion = PhoneAppDetector.localAppVersion(context) ?: ""
            )
        }
    }

    override fun onCapabilityChanged(capabilityInfo: com.google.android.gms.wearable.CapabilityInfo) {
        scope.launch {
            refreshConnectedNodes()
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val dataStr = String(messageEvent.data, Charsets.UTF_8)

        when (path) {
            PATH_PHONE_METRICS -> {
                try {
                    val json = JSONObject(dataStr)
                    val sec = json.optLong("sec", 0L)
                    val meters = json.optDouble("m", 0.0)
                    val cal = json.optInt("cal", 0)
                    val hr = json.optInt("hr", 0)
                    _mirroredMetrics.value = PhoneMirroredMetrics(
                        seconds = sec,
                        meters = meters,
                        calories = cal,
                        heartRate = hr,
                        isPhoneActive = true
                    )
                } catch (e: Exception) {
                    Log.e("PhoneMessagingManager", "Error parsing phone metrics", e)
                }
            }
            PATH_WORKOUT_CONTROL -> {
                try {
                    val json = JSONObject(dataStr)
                    val action = json.optString("action", "")
                    val type = json.optString("workoutType", "Run")
                    scope.launch {
                        _controlActions.emit(Pair(action, type))
                    }
                } catch (e: Exception) {
                    Log.e("PhoneMessagingManager", "Error parsing workout control", e)
                }
            }
        }
    }

    fun broadcastHeartRate(bpm: Int, accuracy: Int) {
        scope.launch {
            val payload = JSONObject().apply {
                put("bpm", bpm)
                put("accuracy", accuracy)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            sendOrEnqueue(PATH_HR_STREAM, "HR", payload)
        }
    }

    fun broadcastCadence(cadence: Int) {
        scope.launch {
            val payload = JSONObject().apply {
                put("cadence", cadence)
                put("timestamp", System.currentTimeMillis())
            }.toString()

            sendOrEnqueue(PATH_CADENCE_STREAM, "CADENCE", payload)
        }
    }

    fun sendWorkoutControl(action: String, workoutType: String) {
        scope.launch {
            val payload = JSONObject().apply {
                put("action", action)
                put("workoutType", workoutType)
                put("source", "watch")
                put("timestamp", System.currentTimeMillis())
            }.toString()

            sendOrEnqueue(PATH_WORKOUT_CONTROL, "CONTROL", payload)
        }
    }

    private suspend fun sendOrEnqueue(path: String, type: String, payload: String) {
        val node = targetPhoneNode
        if (node != null && _connectionStatus.value.isConnected) {
            try {
                messageClient.sendMessage(node.id, path, payload.toByteArray(Charsets.UTF_8)).await()
                return
            } catch (e: Exception) {
                Log.w("PhoneMessagingManager", "Failed to send message live, falling back to queue: ${e.message}")
            }
        }

        // Offline or send failed: queue in Room
        repository.enqueueTelemetry(type, path, payload)
    }

    suspend fun flushOfflineQueue() {
        val node = targetPhoneNode ?: return
        try {
            val pending = repository.getPendingBatch(50)
            if (pending.isEmpty()) return

            val batchArray = JSONArray()
            val idsToMark = mutableListOf<Long>()

            pending.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("path", item.path)
                    put("payload", item.payloadJson)
                    put("timestamp", item.timestamp)
                }
                batchArray.put(obj)
                idsToMark.add(item.id)
            }

            val batchPayload = JSONObject().apply {
                put("items", batchArray)
                put("count", batchArray.length())
            }.toString()

            messageClient.sendMessage(
                node.id,
                PATH_FLUSH_QUEUE,
                batchPayload.toByteArray(Charsets.UTF_8)
            ).await()

            repository.markBatchSynced(idsToMark)
            Log.i("PhoneMessagingManager", "Successfully flushed ${idsToMark.size} queued items to phone")
        } catch (e: Exception) {
            Log.e("PhoneMessagingManager", "Failed to flush offline queue", e)
        }
    }
}
