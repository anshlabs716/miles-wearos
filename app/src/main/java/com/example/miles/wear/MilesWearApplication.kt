package com.example.miles.wear

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.miles.wear.data.local.MilesDatabase
import com.example.miles.wear.data.repository.MilesRepository
import com.example.miles.wear.network.PhoneMessagingManager
import com.example.miles.wear.sensor.SensorTracker

class MilesWearApplication : Application() {

    lateinit var database: MilesDatabase
        private set

    lateinit var repository: MilesRepository
        private set

    lateinit var sensorTracker: SensorTracker
        private set

    lateinit var phoneMessagingManager: PhoneMessagingManager
        private set

    companion object {
        lateinit var instance: MilesWearApplication
            private set
        const val NOTIFICATION_CHANNEL_ID = "miles_workout_channel"
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MilesDatabase.getInstance(this)
        repository = MilesRepository(
            this,
            database.queueDao(),
            database.workoutSessionDao(),
            database.savedPinDao(),
            database.dayStatsDao(),
            database.petDao()
        )
        sensorTracker = SensorTracker(this)
        phoneMessagingManager = PhoneMessagingManager(this, repository).apply {
            initialize()
        }

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
