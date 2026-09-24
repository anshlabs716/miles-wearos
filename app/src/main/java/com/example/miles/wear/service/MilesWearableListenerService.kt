package com.example.miles.wear.service

import android.content.Intent
import android.util.Log
import com.example.miles.wear.MainActivity
import com.example.miles.wear.MilesWearApplication
import com.example.miles.wear.data.model.WorkoutType
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class MilesWearableListenerService : WearableListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val dataStr = String(messageEvent.data, Charsets.UTF_8)
        Log.d("WearableListener", "Message received on path: $path, data: $dataStr")

        when (path) {
            "/miles/workout/control" -> {
                try {
                    val json = JSONObject(dataStr)
                    val action = json.optString("action", "")
                    val typeStr = json.optString("workoutType", "Run")
                    val type = WorkoutType.values().firstOrNull {
                        it.name.equals(typeStr, ignoreCase = true) || it.displayName.equals(typeStr, ignoreCase = true)
                    } ?: WorkoutType.RUN

                    when (action.lowercase()) {
                        "start" -> {
                            val serviceIntent = Intent(this, WorkoutTrackingService::class.java).apply {
                                this.action = WorkoutTrackingService.ACTION_START_MIRRORED
                                putExtra(WorkoutTrackingService.EXTRA_WORKOUT_TYPE, type.name)
                            }
                            startForegroundService(serviceIntent)

                            // Launch watch UI to mirrored screen
                            val activityIntent = Intent(this, MainActivity::class.java).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                putExtra("NAV_TO_MIRRORED", true)
                            }
                            startActivity(activityIntent)
                        }
                        "pause" -> {
                            val intent = Intent(this, WorkoutTrackingService::class.java).apply {
                                this.action = WorkoutTrackingService.ACTION_PAUSE
                            }
                            startService(intent)
                        }
                        "resume" -> {
                            val intent = Intent(this, WorkoutTrackingService::class.java).apply {
                                this.action = WorkoutTrackingService.ACTION_RESUME
                            }
                            startService(intent)
                        }
                        "finish" -> {
                            val intent = Intent(this, WorkoutTrackingService::class.java).apply {
                                this.action = WorkoutTrackingService.ACTION_FINISH
                            }
                            startService(intent)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WearableListener", "Error handling remote workout control", e)
                }
            }
            "/miles/metrics/live" -> {
                // Forwarded directly to phone messaging manager
                try {
                    val json = JSONObject(dataStr)
                    val sec = json.optLong("sec", 0L)
                    val meters = json.optDouble("m", 0.0)
                    val cal = json.optInt("cal", 0)
                    val hr = json.optInt("hr", 0)
                    // Application instance phone messaging updates
                } catch (e: Exception) {
                    Log.e("WearableListener", "Error parsing metrics", e)
                }
            }
        }
    }

    override fun onCapabilityChanged(capabilityInfo: com.google.android.gms.wearable.CapabilityInfo) {
        super.onCapabilityChanged(capabilityInfo)
        scope.launch {
            MilesWearApplication.instance.phoneMessagingManager.refreshConnectedNodes()
        }
    }

    override fun onPeerConnected(node: com.google.android.gms.wearable.Node) {
        super.onPeerConnected(node)
        scope.launch {
            MilesWearApplication.instance.phoneMessagingManager.refreshConnectedNodes()
        }
    }

    override fun onPeerDisconnected(node: com.google.android.gms.wearable.Node) {
        super.onPeerDisconnected(node)
        scope.launch {
            MilesWearApplication.instance.phoneMessagingManager.refreshConnectedNodes()
        }
    }
}
