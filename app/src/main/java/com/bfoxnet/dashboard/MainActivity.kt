package com.bfoxnet.dashboard

import android.app.AppOpsManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bfoxnet.dashboard.admin.DeviceAdminReceiver
import com.bfoxnet.dashboard.services.MonitorService
import com.bfoxnet.dashboard.services.ProtectionAccessibilityService

/**
 * Entry-point activity shown on every launch.
 *
 * The app will NOT proceed to the dashboard unless all three permissions are
 * granted:
 *   1. PACKAGE_USAGE_STATS  – required to display the usage report
 *   2. Device Admin          – required for uninstall prevention
 *   3. Accessibility service – required for Settings-screen protection
 *
 * Each missing permission is requested in order. Once all are granted the
 * user is forwarded to [DashboardActivity].
 */
class MainActivity : AppCompatActivity() {

    private val adminComponent by lazy {
        ComponentName(this, DeviceAdminReceiver::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnGrant).setOnClickListener {
            requestNextMissingPermission()
        }
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            // All permissions in place – start background service and open dashboard
            MonitorService.start(this)
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        } else {
            updateStatusText()
        }
    }

    // -----------------------------------------------------------------------

    private fun allPermissionsGranted(): Boolean =
        hasUsageStatsPermission() && isDeviceAdminActive() && isAccessibilityEnabled()

    private fun updateStatusText() {
        val sb = StringBuilder()
        sb.appendLine(getString(R.string.permissions_required_header))
        sb.appendLine()
        sb.appendLine(if (hasUsageStatsPermission()) "✅ ${getString(R.string.perm_usage_stats)}"
                      else "❌ ${getString(R.string.perm_usage_stats)}")
        sb.appendLine(if (isDeviceAdminActive()) "✅ ${getString(R.string.perm_device_admin)}"
                      else "❌ ${getString(R.string.perm_device_admin)}")
        sb.appendLine(if (isAccessibilityEnabled()) "✅ ${getString(R.string.perm_accessibility)}"
                      else "❌ ${getString(R.string.perm_accessibility)}")
        findViewById<TextView>(R.id.tvStatus).text = sb.toString().trimEnd()
    }

    private fun requestNextMissingPermission() {
        when {
            !hasUsageStatsPermission() -> requestUsageStatsPermission()
            !isDeviceAdminActive()     -> requestDeviceAdmin()
            !isAccessibilityEnabled()  -> requestAccessibility()
        }
    }

    // -- Usage stats --------------------------------------------------------

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun requestUsageStatsPermission() {
        AlertDialog.Builder(this)
            .setTitle(R.string.perm_usage_stats)
            .setMessage(R.string.perm_usage_stats_rationale)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }
            .show()
    }

    // -- Device admin -------------------------------------------------------

    private fun isDeviceAdminActive(): Boolean {
        val dpm = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        return dpm.isAdminActive(adminComponent)
    }

    private fun requestDeviceAdmin() {
        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                getString(R.string.admin_activation_reason))
        }
        startActivity(intent)
    }

    // -- Accessibility ------------------------------------------------------

    private fun isAccessibilityEnabled(): Boolean {
        val service = "${packageName}/${ProtectionAccessibilityService::class.java.canonicalName}"
        val enabled = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.split(":").any { it.equals(service, ignoreCase = true) }
    }

    private fun requestAccessibility() {
        AlertDialog.Builder(this)
            .setTitle(R.string.perm_accessibility)
            .setMessage(R.string.perm_accessibility_rationale)
            .setPositiveButton(R.string.open_settings) { _, _ ->
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
            .show()
    }
}
