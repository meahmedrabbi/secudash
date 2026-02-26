package com.bfoxnet.dashboard.utils

import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import java.util.Calendar

/**
 * Helper that queries [UsageStatsManager] for per-app usage statistics.
 *
 * Usage data is only available when the app holds the PACKAGE_USAGE_STATS
 * permission (granted via Special App Access in Settings).
 */
object UsageStatsHelper {

    data class AppUsage(
        val packageName: String,
        val appName: String,
        val totalTimeInForeground: Long,  // milliseconds
        val lastTimeUsed: Long            // epoch millis
    )

    /**
     * Returns a list of [AppUsage] entries sorted by descending foreground time
     * for the last [days] calendar days.
     */
    fun getUsageStats(context: Context, days: Int = 7): List<AppUsage> {
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

        val calendar = Calendar.getInstance()
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -days)
        val startTime = calendar.timeInMillis

        val stats: Map<String, UsageStats> =
            usageStatsManager.queryAndAggregateUsageStats(startTime, endTime)

        val pm = context.packageManager

        return stats.values
            .filter { it.totalTimeInForeground > 0 }
            .map { usageStat ->
                val appName = try {
                    pm.getApplicationInfo(usageStat.packageName, 0)
                        .loadLabel(pm).toString()
                } catch (e: Exception) {
                    usageStat.packageName
                }
                AppUsage(
                    packageName = usageStat.packageName,
                    appName = appName,
                    totalTimeInForeground = usageStat.totalTimeInForeground,
                    lastTimeUsed = usageStat.lastTimeUsed
                )
            }
            .sortedByDescending { it.totalTimeInForeground }
    }

    /** Formats milliseconds as "Xh Ym" or "Ym Zs". */
    fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours   = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return when {
            hours > 0   -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${seconds}s"
            else        -> "${seconds}s"
        }
    }
}
