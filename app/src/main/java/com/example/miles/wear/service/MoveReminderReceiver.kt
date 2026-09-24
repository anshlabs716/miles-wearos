package com.example.miles.wear.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.miles.wear.MainActivity
import com.example.miles.wear.R
import com.example.miles.wear.engine.MoveReminderManager

/** Fires the "time to move" reminder and re-arms the next one. */
class MoveReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "miles_reminder_channel"

        fun ensureChannel(context: Context) {
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Move reminders",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply { description = "Sitting-streak break reminders" }
                )
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        // Never nag during an actual workout.
        if (WorkoutTrackingService.isWorkoutActive) {
            MoveReminderManager.schedule(context, MoveReminderManager.intervalMinutes(context))
            return
        }
        if (!MoveReminderManager.isEnabled(context)) return

        ensureChannel(context)
        postNotification(context)

        // Reschedule the next reminder (setAndAllowWhileIdle is one-shot).
        MoveReminderManager.schedule(context, MoveReminderManager.intervalMinutes(context))
    }

    private fun postNotification(context: Context) {
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time to move 🏃")
            .setContentText("A little movement breaks the sitting streak. Stand up, stretch, or take a short walk!")
            .setContentIntent(openIntent)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(2, notification)
    }
}