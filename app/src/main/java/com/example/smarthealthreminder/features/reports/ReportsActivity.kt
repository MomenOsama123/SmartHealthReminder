package com.example.smarthealthreminder.features.reports

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.smarthealthreminder.features.navigation.BottomNavHelper
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsActivity : AppCompatActivity() {

    private lateinit var viewModel: ReportViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reportsactivity)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.reportsActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // 1. تجهيز قاعدة البيانات والـ ViewModel
        val database = AppDatabase.getDatabase(this)
        val repository = ReportRepository(database.reportDao())
        val reminderDao = AppDatabase.getDatabase(this).reminderDao() // جيبنا الـ Dao
        val factory = ReportViewModelFactory(repository, reminderDao) // بعتنا الـ Dao للمصنع
        viewModel = ViewModelProvider(this, factory)[ReportViewModel::class.java]

        // 2. تعريف العناصر (Views)
        val fabCreateReport = findViewById<FloatingActionButton>(R.id.fab_create_report)
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        val recyclerReports = findViewById<RecyclerView>(R.id.recycler_reports)

        // عناصر التقرير الديناميكية (تأكد إنك ضفت الـ IDs دي في الـ XML زي ما اتفقنا)
        val tvPercentage = findViewById<TextView>(R.id.tv_percentage)
        val tvAdherenceMessage = findViewById<TextView>(R.id.tv_adherence_message)
        val tvSymptomsOverview = findViewById<TextView>(R.id.tv_symptoms_overview)
        val tvInsight1 = findViewById<TextView>(R.id.tv_insight1)
        val tvInsight2 = findViewById<TextView>(R.id.tv_insight2)
        val progressBar = findViewById<LinearProgressIndicator>(R.id.adherence_progress_bar)
        val tvLastUpdated = findViewById<TextView>(R.id.tv_last_updated)
        val tvStatusTitle = findViewById<TextView>(R.id.tv_status_title)
        val tvStatusDescription = findViewById<TextView>(R.id.tv_status_description)

        // تجهيز الـ RecyclerView (عشان لو حبيت تعرض لستة التقارير القديمة بعدين)
        recyclerReports.layoutManager = LinearLayoutManager(this)

        // لما نضغط على زرار (+) هنخليه يعمل تقرير تجريبي ويحفظه في الداتابيز
        fabCreateReport.setOnClickListener {
            fabCreateReport.setOnClickListener {
                viewModel.generateRealReport() // دي الدالة اللي عملناها وبتحسب الداتا الحقيقية
                Toast.makeText(this, "Generating Real Report...", Toast.LENGTH_SHORT).show()
            }
        }

        // تشغيل الشريط السفلي
        BottomNavHelper.setup(
            activity = this,
            bottomNavigation = bottomNavigation
        )

        // 4. مراقبة الداتابيز (تحديث الشاشة تلقائياً)
        lifecycleScope.launch {
            // الكود ده بيشتغل لوحده أول ما أي تقرير جديد يتضاف في الداتابيز
            viewModel.allReports.collect { reportsList ->

                if (reportsList.isNotEmpty()) {
                    // هناخد أحدث تقرير (أول واحد في اللستة)
                    val latestReport = reportsList.first()

                    // تحديث الأرقام والنصوص في الشاشة برمجياً
                    tvPercentage.text = getString(R.string.percentage_format, latestReport.adherencePercentage)

                    // تحديث الرسالة
                    tvAdherenceMessage?.text = getString(
                        R.string.report_adherence_summary,
                        latestReport.adherencePercentage,
                        latestReport.missedDoses
                    )

                    // تحديث الأعراض والنصائح
                    tvSymptomsOverview?.text = latestReport.symptomsOverview
                    tvInsight1?.text = latestReport.aiInsight1
                    tvInsight2?.text = latestReport.aiInsight2

                    // تحديث الحالة بناءً على النسبة
                    if (latestReport.adherencePercentage >= 80) {
                        tvStatusTitle?.text = getString(R.string.status_stable_title)
                        tvStatusDescription?.text = getString(R.string.status_stable_description)
                        progressBar?.setIndicatorColor(getColor(R.color.primary_green))
                    } else if (latestReport.adherencePercentage >= 50) {
                        tvStatusTitle?.text = getString(R.string.status_fair_title)
                        tvStatusDescription?.text = getString(R.string.status_fair_description)
                        progressBar?.setIndicatorColor(getColor(R.color.pending))
                    } else {
                        tvStatusTitle?.text = getString(R.string.status_action_title)
                        tvStatusDescription?.text = getString(R.string.status_action_description)
                        progressBar?.setIndicatorColor(getColor(R.color.error_red))
                    }

                    // تحديث شريط التقدم برمجياً
                    progressBar?.setProgress(latestReport.adherencePercentage, true)

                    // تحديث التاريخ
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val formattedDate = sdf.format(Date(latestReport.createdAt))
                    tvLastUpdated?.text = getString(R.string.last_updated_label, formattedDate)
                } else {
                    // لو مفيش تقارير، اعمل واحد فوراً
                    viewModel.generateRealReport()
                }
            }
        }
    }
}