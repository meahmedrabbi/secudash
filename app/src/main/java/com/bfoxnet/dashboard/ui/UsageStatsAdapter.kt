package com.bfoxnet.dashboard.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bfoxnet.dashboard.R
import com.bfoxnet.dashboard.utils.UsageStatsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [RecyclerView.Adapter] that displays per-app usage statistics in the dashboard.
 * Uses [DiffUtil] for efficient, targeted updates instead of a full rebind.
 */
class UsageStatsAdapter(
    private var items: List<UsageStatsHelper.AppUsage> = emptyList()
) : RecyclerView.Adapter<UsageStatsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView        = view.findViewById(R.id.tvRank)
        val tvAppName: TextView     = view.findViewById(R.id.tvAppName)
        val tvPackageName: TextView = view.findViewById(R.id.tvPackageName)
        val tvDuration: TextView    = view.findViewById(R.id.tvDuration)
        val tvLastUsed: TextView    = view.findViewById(R.id.tvLastUsed)
    }

    fun updateData(newItems: List<UsageStatsHelper.AppUsage>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos].packageName == newItems[newPos].packageName
            override fun areContentsTheSame(oldPos: Int, newPos: Int) =
                items[oldPos] == newItems[newPos]
        })
        items = newItems
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_app_usage, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())

        holder.tvRank.text        = "#${position + 1}"
        holder.tvAppName.text     = item.appName
        holder.tvPackageName.text = item.packageName
        holder.tvDuration.text    = UsageStatsHelper.formatDuration(item.totalTimeInForeground)
        holder.tvLastUsed.text    = holder.itemView.context.getString(
            R.string.last_used_format,
            dateFormat.format(Date(item.lastTimeUsed))
        )
    }

    override fun getItemCount(): Int = items.size
}
