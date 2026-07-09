package com.example.smarthealthreminder.features.reports

import android.content.ContentValues
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    private val viewModel: ReportViewModel by viewModel()

    // Helper data structure to hold daily metrics for the UI calculation
    data class DayMedicationSummary(val total: Int, val taken: Int, val missed: Int)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply dynamic WindowInsets safely to avoid toolbar overlapping issues
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize UI component reference mappings from compiled layouts
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

        // Map Weekday view IDs inside a sequential list array structure
        val dayViews = listOf(
            view.findViewById<TextView>(R.id.day_mon),
            view.findViewById<TextView>(R.id.day_tue),
            view.findViewById<TextView>(R.id.day_wed),
            view.findViewById<TextView>(R.id.day_thu),
            view.findViewById<TextView>(R.id.day_fri),
            view.findViewById<TextView>(R.id.day_sat),
            view.findViewById<TextView>(R.id.day_sun)
        )

        // Updates the day selection styles and text metrics inside the card block dynamically
        fun updateDaysUI(selectedDayIndex: Int, remindersMap: Map<Int, DayMedicationSummary>) {
            dayViews.forEachIndexed { index, textView ->
                val summary = remindersMap[index]

                when {
                    // Current Active Selection State
                    index == selectedDayIndex -> {
                        textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        textView?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_pill)
                        textView?.backgroundTintList = ContextCompat.getColorStateList(requireContext(), R.color.primary_green)

                        tvSymptomsOverview?.text = if (summary != null) {
                            "Total: ${summary.total} | ✅ Taken: ${summary.taken} | ❌ Missed: ${summary.missed}"
                        } else {
                            "No medications scheduled for this day."
                        }
                    }
                    // Day contains database information and missed doses are zero (Stable Green)
                    summary != null && summary.missed == 0 -> {
                        textView?.background = null
                        textView?.backgroundTintList = null
                        textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                    }
                    // Day contains database information and missed doses exist (Warning Red)
                    summary != null && summary.missed > 0 -> {
                        textView?.background = null
                        textView?.backgroundTintList = null
                        textView?.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                    }
                    // Default fallback state mapping dynamic theme color resources
                    else -> {
                        val typedValue = TypedValue()
                        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurfaceVariant, typedValue, true)
                        textView?.setTextColor(typedValue.data)
                        textView?.background = null
                        textView?.backgroundTintList = null
                    }
                }
            }

            // Revert summary space text back to fallback label if layout initialization index is passed
            if (selectedDayIndex == -1) {
                tvSymptomsOverview?.text = "Select a day to view your medication history."
            }
        }

        recyclerReports.layoutManager = LinearLayoutManager(requireContext())

        // Trigger manual analytics calculations on back-end IO thread pools
        fabCreateReport.setOnClickListener {
            viewModel.generateRealReport()
            Toast.makeText(requireContext(), getString(R.string.generating_report), Toast.LENGTH_SHORT).show()
        }

        // Setup PDF download print engine routine
        val downloadBtn = view.findViewById<Button>(R.id.download_btn)
        downloadBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.preparing_report), Toast.LENGTH_SHORT).show()
            val fileName = "Trusta_Health_Report_${System.currentTimeMillis()}.pdf"
            lifecycleScope.launch(Dispatchers.IO) {
                generateAndSavePdfDirectly(fileName)
            }
        }

        // Live observation pipeline gathering historical system reports and dynamic compliance indexes
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allReports.collect { reportsList ->
                if (reportsList.isNotEmpty()) {
                    val latestReport = reportsList.first()

                    // Keep AI Insights from the latest generated report
                    tvInsight1?.text = latestReport.aiInsight1
                    tvInsight2?.text = latestReport.aiInsight2

                    // Show the timestamp of the last generated report
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val lastUpdatedText = getString(R.string.last_updated_label, sdf.format(Date(latestReport.createdAt)))
                    tvLastUpdated?.text = lastUpdatedText

                    // Fetch real database reminder tracking streams for the current trailing week
                    val sevenDaysAgoTimestamp = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)

                    viewModel.getRecentReminders(sevenDaysAgoTimestamp).collect { remindersList ->

                        // =======================================================
                        // 1. Calculate Live Adherence Percentage
                        // =======================================================
                        val totalDosesThisWeek = remindersList.size
                        val takenDosesThisWeek = remindersList.count { it.status.equals("Completed", ignoreCase = true) }
                        val missedDosesThisWeek = totalDosesThisWeek - takenDosesThisWeek

                        // Calculate percentage, preventing division by zero
                        val calculatedPercentage = if (totalDosesThisWeek > 0) {
                            (takenDosesThisWeek * 100) / totalDosesThisWeek
                        } else {
                            0
                        }

                        // Update Adherence Progress UI (Card 2)
                        tvPercentage.text = getString(R.string.percentage_format, calculatedPercentage)
                        tvAdherenceMessage?.text = getString(R.string.report_adherence_summary, calculatedPercentage, missedDosesThisWeek)
                        progressBar?.setProgress(calculatedPercentage, true)

                        // Update Health Status UI (Card 1) based on the newly calculated percentage
                        if (calculatedPercentage >= 80) {
                            tvStatusTitle?.text = getString(R.string.status_stable_title)
                            tvStatusDescription?.text = getString(R.string.status_stable_description)
                            progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                        } else if (calculatedPercentage >= 50) {
                            tvStatusTitle?.text = getString(R.string.status_fair_title)
                            tvStatusDescription?.text = getString(R.string.status_fair_description)
                            progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.pending))
                        } else {
                            tvStatusTitle?.text = getString(R.string.status_action_title)
                            tvStatusDescription?.text = getString(R.string.status_action_description)
                            progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                        }

                        // =======================================================
                        // 2. Group Reminders for Daily Breakdown (Card 3)
                        // =======================================================
                        val realRemindersMap = mutableMapOf<Int, DayMedicationSummary>()

                        // Group reminders matching day index arrays using the date String
                        val groupedReminders = remindersList.groupBy { reminder ->
                            getDayIndexFromDateString(reminder.date)
                        }

                        // Process nested arrays to compute adherence metrics dynamically
                        groupedReminders.forEach { (dayIndex, dayReminders) ->
                            val total = dayReminders.size
                            val taken = dayReminders.count { it.status.equals("Completed", ignoreCase = true) }
                            val missed = total - taken

                            realRemindersMap[dayIndex] = DayMedicationSummary(total, taken, missed)
                        }

                        // Attach listeners and setup starting view configurations
                        updateDaysUI(-1, realRemindersMap)
                        dayViews.forEachIndexed { index, textView ->
                            textView?.setOnClickListener {
                                updateDaysUI(index, realRemindersMap)
                            }
                        }
                    }

                    // Auto-generate a new report if 7 days have passed since the last one
                    val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L
                    if (System.currentTimeMillis() - latestReport.createdAt > sevenDaysInMillis) {
                        viewModel.generateRealReport()
                    }
                } else {
                    // Generate initial report if the database is empty
                    viewModel.generateRealReport()
                }
            }
        }
    }

    // Helper function to convert String date (e.g., "dd/MM/yyyy" or "yyyy-MM-dd") into weekday index (0-6)
    private fun getDayIndexFromDateString(dateString: String?): Int {
        if (dateString.isNullOrEmpty()) return 0

        val formats = arrayOf("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "yyyy/MM/dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) {
                    val calendar = Calendar.getInstance()
                    calendar.time = date
                    val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                    return when (dayOfWeek) {
                        Calendar.MONDAY -> 0
                        Calendar.TUESDAY -> 1
                        Calendar.WEDNESDAY -> 2
                        Calendar.THURSDAY -> 3
                        Calendar.FRIDAY -> 4
                        Calendar.SATURDAY -> 5
                        else -> 6 // Calendar.SUNDAY
                    }
                }
            } catch (e: Exception) {
                // Ignore format mismatch and try the next one
            }
        }
        return 0 // Fallback if parsing fails
    }

    // PDF Canvas generator processing routine
    private suspend fun generateAndSavePdfDirectly(fileName: String) {
        try {
            val statusTitle: String
            val percentage: String
            val adherenceMsg: String
            val insight1: String
            val insight2: String

            withContext(Dispatchers.Main) {
                val view = requireView()
                statusTitle = view.findViewById<TextView>(R.id.tv_status_title)?.text.toString()
                percentage = view.findViewById<TextView>(R.id.tv_percentage)?.text.toString()
                adherenceMsg = view.findViewById<TextView>(R.id.tv_adherence_message)?.text.toString()
                insight1 = view.findViewById<TextView>(R.id.tv_insight1)?.text.toString()
                insight2 = view.findViewById<TextView>(R.id.tv_insight2)?.text.toString()
            }

            val document = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            canvas.drawColor(Color.WHITE)

            val titlePaint = Paint().apply {
                color = Color.BLACK
                textSize = 26f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }

            val sectionPaint = Paint().apply {
                color = Color.rgb(46, 125, 50)
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            }

            val textPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 2f
            }

            var currentY = 60f
            val marginX = 50f
            val contentWidth = pageWidth - (marginX * 2).toInt()

            canvas.drawText(getString(R.string.app_name), pageWidth / 2f, currentY, titlePaint)
            currentY += 30f

            titlePaint.textSize = 16f
            titlePaint.color = Color.GRAY
            canvas.drawText(getString(R.string.pdf_report_title), pageWidth / 2f, currentY, titlePaint)
            currentY += 40f

            val dateSdf = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault())
            val currentDate = dateSdf.format(Date())
            canvas.drawText(getString(R.string.pdf_generated_on, currentDate), marginX, currentY, textPaint)
            currentY += 20f

            canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
            currentY += 40f

            canvas.drawText(getString(R.string.pdf_section_health_status), marginX, currentY, sectionPaint)
            currentY += 30f
            canvas.drawText(statusTitle, marginX + 15f, currentY, textPaint)
            currentY += 40f

            canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
            currentY += 40f

            canvas.drawText(getString(R.string.pdf_section_adherence), marginX, currentY, sectionPaint)
            currentY += 30f

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(getString(R.string.pdf_adherence_rate, percentage), marginX + 15f, currentY, textPaint)
            currentY += 25f

            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

            val adherenceLayout = StaticLayout.Builder.obtain(adherenceMsg, 0, adherenceMsg.length, textPaint, contentWidth - 15)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(marginX + 15f, currentY)
            adherenceLayout.draw(canvas)
            canvas.restore()
            currentY += adherenceLayout.height + 40f

            canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
            currentY += 40f

            canvas.drawText(getString(R.string.pdf_section_insights), marginX, currentY, sectionPaint)
            currentY += 30f

            val insight1Text = "• $insight1"
            val insight1Layout = StaticLayout.Builder.obtain(insight1Text, 0, insight1Text.length, textPaint, contentWidth - 15).build()
            canvas.save()
            canvas.translate(marginX + 15f, currentY)
            insight1Layout.draw(canvas)
            canvas.restore()
            currentY += insight1Layout.height + 20f

            val insight2Text = "• $insight2"
            val insight2Layout = StaticLayout.Builder.obtain(insight2Text, 0, insight2Text.length, textPaint, contentWidth - 15).build()
            canvas.save()
            canvas.translate(marginX + 15f, currentY)
            insight2Layout.draw(canvas)
            canvas.restore()

            val footerPaint = Paint().apply {
                color = Color.LTGRAY
                textSize = 12f
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText(getString(R.string.pdf_footer, getString(R.string.app_name)), pageWidth / 2f, pageHeight - 40f, footerPaint)

            document.finishPage(page)

            // Scans and saves Document bytes based on active SDK versions safely
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }

                val uri = requireContext().contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), getString(R.string.pdf_saved_success), Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(downloadsDir, fileName)
                FileOutputStream(file).use { outputStream ->
                    document.writeTo(outputStream)
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), getString(R.string.pdf_saved_success), Toast.LENGTH_LONG).show()
                }
            }

            document.close()

        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), getString(R.string.pdf_save_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}