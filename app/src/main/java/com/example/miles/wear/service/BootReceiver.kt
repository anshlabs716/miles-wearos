package com.example.miles.wear.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.miles.wear.engine.MoveReminderManager

/** Re-arms move reminders after a device reboot. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            MoveReminderManager.applySetting(context)
        }
    }
}