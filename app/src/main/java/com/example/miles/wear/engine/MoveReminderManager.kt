package com.example.miles.wear.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.example.miles.wear.service.MoveReminderReceiver

/**
 * Sedentary "move" reminders every N minutes, skipping periods when a
 * workout is actually active. Ported from the MILES phone app.
 */
object MoveReminderManager {

    private const val PREFS = "miles_wear_prefs"
    const val REQUEST_CODE = 9001

    fun schedule(context: Context, intervalMinutes: Int) {
        val min = intervalMinutes.coerceIn(15, 480)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pi = pendingIntent(context)
        val trigger = SystemClock.elapsedRealtime() + min * 60_000L
        try {
            am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, trigger, pi)
        } catch (_: Exception) {
            try {
                am.set(AlarmManager.ELAPSED_REALTIME, trigger, pi)
            } catch (_: Exception) {
            }
        }
    }

    fun cancel(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        am.cancel(pendingIntent(context))
    }

    fun isEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getBoolean("move_reminder_enabled", false)

    fun intervalMinutes(context: Context): Int =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt("move_reminder_interval_min", 60)

    /** Call whenever the setting changes to keep the schedule in sync. */
    fun applySetting(context: Context) {
        if (isEnabled(context)) {
            schedule(context, intervalMinutes(context))
        } else {
            cancel(context)
        }
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MoveReminderReceiver::class.java).apply {
            action = "com.example.miles.wear.action.MOVE_REMINDER"
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}