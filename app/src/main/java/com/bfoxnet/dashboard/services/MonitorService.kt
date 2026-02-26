package com.bfoxnet.dashboard.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.bfoxnet.dashboard.DashboardActivity
import com.bfoxnet.dashboard.R

/**
 * Persistent foreground service that keeps the app alive in the background.
 *
 * The service is started with [START_STICKY] so that the OS re-creates it
 * if it is ever killed.  The [BootReceiver] starts it again after reboot.
 *
 * Responsibilities while running:
 *  - Maintains a visible foreground notification (required by Android 8+).
 *  - Periodically verifies that Device Admin and Accessibility protections are
 *    still active and prompts the user to re-enable them if they are not.
 */
class MonitorService : Service() {

    companion object {
        private const val CHANNEL_ID      = "secudash_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val CHECK_INTERVAL_MS = 30_000L  // 30 seconds

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            context.startForegroundService(intent)
        }
    }

    private val checkRunnable = object : Runnable {
        override fun run() {
            checkProtections()
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())

    // -----------------------------------------------------------------------

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        handler.post(checkRunnable)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Ensure foreground state is maintained on every (re-)start
        startForeground(NOTIFICATION_ID, buildNotification())
        return START_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkRunnable)
        super.onDestroy()
        // Restart immediately if destroyed unexpectedly
        start(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -----------------------------------------------------------------------

    private fun checkProtections() {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as android.app.admin.DevicePolicyManager
        val adminComponent = android.content.ComponentName(this,
            com.bfoxnet.dashboard.admin.DeviceAdminReceiver::class.java)

        // If Device Admin was removed, prompt the user to re-enable it
        if (!dpm.isAdminActive(adminComponent)) {
            val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                putExtra(android.app.admin.DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                    getString(R.string.admin_activation_reason))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }

        // If Accessibility service is disabled, prompt the user to re-enable it
        if (!isAccessibilityServiceEnabled()) {
            val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "${packageName}/${ProtectionAccessibilityService::class.java.canonicalName}"
        val enabled = android.provider.Settings.Secure.getString(
            contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").any { it.equals(service, ignoreCase = true) }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, DashboardActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.monitor_notification_title))
            .setContentText(getString(R.string.monitor_notification_text))
            .setSmallIcon(android.R.drawable.ic_menu_manage)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.monitor_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.monitor_channel_description)
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
