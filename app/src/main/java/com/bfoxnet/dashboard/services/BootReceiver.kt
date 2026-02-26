package com.bfoxnet.dashboard.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Restarts [MonitorService] after the device boots or after the app is updated.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MonitorService.start(context)
        }
    }
}
