package com.example.smarthealthreminder.features.Profileinfo.reports

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.repository.HealthRepository
import com.example.smarthealthreminder.features.adapter.ScheduleAdapter
import com.example.smarthealthreminder.features.model.ScheduleItem
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModel
import com.example.smarthealthreminder.features.ui.viewmodel.HealthViewModelFactory
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ReportsActivity : AppCompatActivity() {

    private lateinit var viewModel: HealthViewModel
    private lateinit var scheduleAdapter: ScheduleAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportsactivity)

        val db = AppDatabase.getDatabase(this)
        val repository = HealthRepository(db)
        viewModel = HealthViewModelFactory(repository).create(HealthViewModel::class.java)

        val btnBack = findViewById<ImageView>(R.id.btn_back)
        val btnDownload = findViewById<Button>(R.id.download_btn)

        btnBack.setOnClickListener { finish() }

        btnDownload.setOnClickListener {
            Toast.makeText(this, "Downloading Report...", Toast.LENGTH_SHORT).show()
        }

        val fabCreateReport = findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_create_report)
        fabCreateReport?.setOnClickListener {
            showCreateReportDialog()
        }

        setupRecyclerView()
        observeReports()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_reports)
        scheduleAdapter = ScheduleAdapter()
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = scheduleAdapter
    }

    private fun observeReports() {
        lifecycleScope.launch {
            viewModel.allReports.collect { reports ->
                val items = reports.map { report ->
                    ScheduleItem(
                        id = report.id,
                        title = report.title,
                        date = report.date ?: "",
                        time = "Report",
                        category = "Report",
                        priority = "NORMAL",
                        status = "Completed",
                        isAlarm = false,
                        itemType = ScheduleItem.TYPE_REPORT
                    )
                }
                scheduleAdapter.submitList(items)
            }
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        BottomNavHelper.setup(
            activity = this,
            bottomNavigation = bottomNav
        )
    }

    private fun showCreateReportDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_create_report, null)
        val etTitle = dialogView.findViewById<EditText>(R.id.et_report_title)
        val etDescription = dialogView.findViewById<EditText>(R.id.et_report_description)

        AlertDialog.Builder(this)
            .setTitle("Create Report")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = etTitle.text.toString().trim()
                val description = etDescription.text.toString().trim()
                if (title.isNotEmpty()) {
                    val report = ReportEntity(
                        id = UUID.randomUUID().toString(),
                        title = title,
                        description = description,
                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    )
                    lifecycleScope.launch {
                        viewModel.addReport(report)
                        Toast.makeText(this@ReportsActivity, "Report '$title' created", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Title is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
