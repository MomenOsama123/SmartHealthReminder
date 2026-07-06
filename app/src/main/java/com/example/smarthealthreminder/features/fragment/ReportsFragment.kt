package com.example.smarthealthreminder.features.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.example.smarthealthreminder.features.reports.ReportViewModel
import com.example.smarthealthreminder.features.reports.ReportViewModelFactory
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var viewModel: ReportViewModel

    // 1. ربط ملف الـ XML بالفراجمنت
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // لو غيرت اسم ملف الـ XML لـ fragment_reports، عدله هنا. لو لسه زي ما هو سيبه
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    // 2. كتابة الأكواد والمنطق هنا
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // تظبيط الحواف
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.reportsActivity)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // تجهيز قاعدة البيانات والـ ViewModel (استخدمنا requireContext بدل this)
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ReportRepository(database.reportDao())
        val reminderDao = database.reminderDao()
        val factory = ReportViewModelFactory(repository, reminderDao)
        viewModel = ViewModelProvider(this, factory)[ReportViewModel::class.java]

        // تعريف العناصر باستخدام view.findViewById
        val fabCreateReport = view.findViewById<FloatingActionButton>(R.id.fab_create_report)
        val recyclerReports = view.findViewById<RecyclerView>(R.id.recycler_reports)

        val tvPercentage = view.findViewById<TextView>(R.id.tv_percentage)
        val tvAdherenceMessage = view.findViewById<TextView>(R.id.tv_adherence_message)
        val tvSymptomsOverview = view.findViewById<TextView>(R.id.tv_symptoms_overview)
        val tvInsight1 = view.findViewById<TextView>(R.id.tv_insight1)
        val tvInsight2 = view.findViewById<TextView>(R.id.tv_insight2)
        val progressBar = view.findViewById<LinearProgressIndicator>(R.id.adherence_progress_bar)
        val tvLastUpdated = view.findViewById<TextView>(R.id.tv_last_updated)
        val tvStatusTitle = view.findViewById<TextView>(R.id.tv_status_title)
        val tvStatusDescription = view.findViewById<TextView>(R.id.tv_status_description)

        recyclerReports.layoutManager = LinearLayoutManager(requireContext())

        // زرار إنشاء التقرير الخاص بالصفحة دي (صلحنا الغلطة القديمة)
        fabCreateReport.setOnClickListener {
            viewModel.generateRealReport()
            Toast.makeText(requireContext(), "Generating Real Report...", Toast.LENGTH_SHORT).show()
        }

        // ملاحظة: مسحنا كود BottomNavHelper من هنا لأن الـ Activity الأم هي اللي بتديره

        // مراقبة الداتابيز (استخدمنا viewLifecycleOwner.lifecycleScope بدل lifecycleScope بس)
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allReports.collect { reportsList ->

                if (reportsList.isNotEmpty()) {
                    val latestReport = reportsList.first()

                    tvPercentage.text = "${latestReport.adherencePercentage}%"
                    tvAdherenceMessage?.text = "You took ${latestReport.adherencePercentage}% of your medications this week. You missed only ${latestReport.missedDoses} doses."
                    tvSymptomsOverview?.text = latestReport.symptomsOverview
                    tvInsight1?.text = latestReport.aiInsight1
                    tvInsight2?.text = latestReport.aiInsight2

                    // استخدمنا ContextCompat.getColor عشان نلون شريط التقدم جوه الفراجمنت
                    if (latestReport.adherencePercentage >= 80) {
                        tvStatusTitle?.text = "Your condition is stable"
                        tvStatusDescription?.text = "Excellent adherence this week. Your health vitals are showing positive trends."
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                    } else if (latestReport.adherencePercentage >= 50) {
                        tvStatusTitle?.text = "Your condition is fair"
                        tvStatusDescription?.text = "Adherence could be improved. Consistency is key to maintaining stable health."
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.pending))
                    } else {
                        tvStatusTitle?.text = "Action required"
                        tvStatusDescription?.text = "Low adherence detected. Please try to follow your medication schedule more closely."
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                    }

                    progressBar?.setProgress(latestReport.adherencePercentage, true)

                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val lastUpdatedText = "Last updated: ${sdf.format(Date(latestReport.createdAt))}"
                    tvLastUpdated?.text = lastUpdatedText
                } else {
                    viewModel.generateRealReport()
                }
            }
        }
    }
}