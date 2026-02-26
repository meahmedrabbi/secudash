package com.bfoxnet.dashboard

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bfoxnet.dashboard.services.MonitorService
import com.bfoxnet.dashboard.ui.UsageStatsAdapter
import com.bfoxnet.dashboard.utils.UsageStatsHelper

/**
 * Main dashboard that shows per-app usage statistics.
 *
 * The user can filter by time range (Today / Last 7 days / Last 30 days).
 * Usage data is fetched from [UsageStatsHelper] on a background thread and
 * posted back to the UI thread for display.
 */
class DashboardActivity : AppCompatActivity() {

    private lateinit var adapter: UsageStatsAdapter
    private lateinit var tvEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        // Ensure background service is running
        MonitorService.start(this)

        setupRecyclerView()
        setupSpinner()
    }

    override fun onResume() {
        super.onResume()
        loadUsageStats(days = 7)
    }

    // -----------------------------------------------------------------------

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.rvAppUsage)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        adapter      = UsageStatsAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        recyclerView.adapter = adapter
    }

    private fun setupSpinner() {
        val spinner = findViewById<Spinner>(R.id.spinnerRange)
        val options = resources.getStringArray(R.array.time_range_options)
        val days    = intArrayOf(1, 7, 30)

        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        spinner.setSelection(1) // default: 7 days

        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                loadUsageStats(days[pos])
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun loadUsageStats(days: Int) {
        // Run on background thread to avoid ANR
        Thread {
            val stats = UsageStatsHelper.getUsageStats(this, days)
            runOnUiThread {
                if (stats.isEmpty()) {
                    tvEmptyState.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    tvEmptyState.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    adapter.updateData(stats)
                }
            }
        }.start()
    }
}
