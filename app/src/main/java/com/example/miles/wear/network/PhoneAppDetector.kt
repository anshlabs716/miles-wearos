package com.example.miles.wear.network

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Detects the MILES phone app (`com.aistudio.miles.track`):
 * - **locally** — same device that this wear app runs on (PackageManager)
 * - **nearby** — reachable over the Wearable data layer (see
 *   PhoneMessagingManager capability probe for `miles_phone_app`)
 * All values are real; version comes straight from the installed package.
 */
object PhoneAppDetector {

    const val PHONE_PACKAGE = "com.aistudio.miles.track"

    fun localAppInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PHONE_PACKAGE, 0) != null
    } catch (_: PackageManager.NameNotFoundException) {
        false
    } catch (_: Exception) {
        false
    }

    /** Version string of the installed phone app, or null if not installed. */
    fun localAppVersion(context: Context): String? = try {
        context.packageManager.getPackageInfo(PHONE_PACKAGE, 0).versionName
    } catch (_: Exception) {
        null
    }

    /** Launches the phone app if installed on this device. Returns true if launched. */
    fun launchPhoneApp(context: Context): Boolean {
        return try {
            val launch = context.packageManager.getLaunchIntentForPackage(PHONE_PACKAGE)
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                android.util.Log.i("MilesAppDetect", "opened MILES phone app")
                true
            } else false
        } catch (_: Exception) {
            false
        }
    }
}