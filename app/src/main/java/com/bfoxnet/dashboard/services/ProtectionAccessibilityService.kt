package com.bfoxnet.dashboard.services

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.bfoxnet.dashboard.admin.DeviceAdminReceiver

/**
 * Accessibility service that monitors window events to detect and block attempts
 * to remove Device Admin or disable this accessibility service via Settings.
 *
 * Monitored scenarios:
 *  1. User opens "Device Admin Apps" settings page and tries to deactivate our app.
 *  2. User opens "Installed Accessibility Services" settings page and tries to
 *     disable our service.
 *  3. User navigates to App Info for com.bfoxnet.dashboard and attempts to
 *     uninstall the app.
 *
 * In all three cases the service immediately closes the dangerous Settings screen
 * and re-opens the relevant activation UI so the protection cannot be bypassed.
 */
class ProtectionAccessibilityService : AccessibilityService() {

    companion object {
        // Settings package names across AOSP / OEM variants
        private val SETTINGS_PACKAGES = setOf(
            "com.android.settings",
            "com.samsung.android.settings",
            "com.miui.securitycenter",
            "com.huawei.systemmanager"
        )

        // Class-name fragments that indicate dangerous Settings screens
        private val DANGEROUS_CLASSES = listOf(
            // Device admin deactivation screen
            "DeviceAdminAdd",
            // Accessibility toggle screens
            "AccessibilitySettings",
            "ToggleAccessibilityServicePreferenceFragment",
            // App-info / uninstall screens
            "AppInfoDashboardFragment",
            "InstalledAppDetails",
            "UninstalledAppDetails",
            "AppInfoBase"
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val className   = event.className?.toString()  ?: return

        // Only react to window-state-change events from Settings
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (packageName !in SETTINGS_PACKAGES) return

        val isDangerous = DANGEROUS_CLASSES.any { fragment ->
            className.contains(fragment, ignoreCase = true)
        }

        if (isDangerous) {
            blockDangerousScreen(packageName, className)
        }
    }

    override fun onInterrupt() { /* required override – nothing to do */ }

    // -----------------------------------------------------------------------

    private fun blockDangerousScreen(packageName: String, className: String) {
        // Press HOME to leave the dangerous screen immediately
        performGlobalAction(GLOBAL_ACTION_HOME)

        when {
            className.contains("DeviceAdmin", ignoreCase = true) -> reActivateDeviceAdmin()
            className.contains("Accessibility", ignoreCase = true) -> reActivateAccessibility()
            else -> {
                // App-info / uninstall screen – just go home; service + admin re-activate anyway
                MonitorService.start(applicationContext)
            }
        }
    }

    /** Re-open the Device Admin activation screen so admin stays enabled. */
    private fun reActivateDeviceAdmin() {
        val intent = Intent(android.app.admin.DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(
                android.app.admin.DevicePolicyManager.EXTRA_DEVICE_ADMIN,
                ComponentName(applicationContext, DeviceAdminReceiver::class.java)
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }

    /** Re-open the Accessibility settings so the user cannot disable the service. */
    private fun reActivateAccessibility() {
        val intent = Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(intent)
    }
}
