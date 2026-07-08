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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.AppDatabase
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportsFragment : Fragment() {

    private lateinit var viewModel: ReportViewModel

    // 1. Inflate the layout for this fragment
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_reports, container, false)
    }

    // 2. Setup logic and UI interactions
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Apply WindowInsets safely to the root view to avoid covering content
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Setup Database, Repository, and ViewModel
        val database = AppDatabase.getDatabase(requireContext())
        val repository = ReportRepository(database.reportDao())
        val reminderDao = database.reminderDao()
        val factory = ReportViewModelFactory(requireActivity().application, repository, reminderDao)
        viewModel = ViewModelProvider(this, factory)[ReportViewModel::class.java]

        // Initialize UI components
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

        // Setup RecyclerView
        recyclerReports.layoutManager = LinearLayoutManager(requireContext())

        // Handle Floating Action Button click to generate a new report
        fabCreateReport.setOnClickListener {
            viewModel.generateRealReport()
            Toast.makeText(requireContext(), getString(R.string.generating_report), Toast.LENGTH_SHORT).show()
        }

        // Handle Download Button click to generate and save PDF
        val downloadBtn = view.findViewById<Button>(R.id.download_btn)
        downloadBtn.setOnClickListener {
            Toast.makeText(requireContext(), getString(R.string.preparing_report), Toast.LENGTH_SHORT).show()
            val fileName = "Trusta_Health_Report_${System.currentTimeMillis()}.pdf"

            // Run PDF generation in a background thread to prevent UI freezing
            lifecycleScope.launch(Dispatchers.IO) {
                generateAndSavePdfDirectly(fileName)
            }
        }

        // Observe the database for latest report updates
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.allReports.collect { reportsList ->
                if (reportsList.isNotEmpty()) {
                    val latestReport = reportsList.first()

                    // Update UI with the latest report data
                    tvPercentage.text = getString(R.string.percentage_format, latestReport.adherencePercentage)
                    tvAdherenceMessage?.text = getString(R.string.report_adherence_summary, latestReport.adherencePercentage, latestReport.missedDoses)
                    tvSymptomsOverview?.text = latestReport.symptomsOverview
                    tvInsight1?.text = latestReport.aiInsight1
                    tvInsight2?.text = latestReport.aiInsight2

                    // Adjust progress bar color and text based on adherence rate
                    if (latestReport.adherencePercentage >= 80) {
                        tvStatusTitle?.text = getString(R.string.status_stable_title)
                        tvStatusDescription?.text = getString(R.string.status_stable_description)
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.primary_green))
                    } else if (latestReport.adherencePercentage >= 50) {
                        tvStatusTitle?.text = getString(R.string.status_fair_title)
                        tvStatusDescription?.text = getString(R.string.status_fair_description)
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.pending))
                    } else {
                        tvStatusTitle?.text = getString(R.string.status_action_title)
                        tvStatusDescription?.text = getString(R.string.status_action_description)
                        progressBar?.setIndicatorColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                    }

                    progressBar?.setProgress(latestReport.adherencePercentage, true)

                    // Format and display the last updated date
                    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                    val lastUpdatedText = getString(R.string.last_updated_label, sdf.format(Date(latestReport.createdAt)))
                    tvLastUpdated?.text = lastUpdatedText
                } else {
                    // Generate an initial report if the list is empty
                    viewModel.generateRealReport()
                }
            }
        }
    }

    // Function to draw and save a professional A4 PDF report
    private suspend fun generateAndSavePdfDirectly(fileName: String) {
        withContext(Dispatchers.Main) {
            try {
                val view = requireView()

                // 1. Gather data directly from the UI elements
                val statusTitle = view.findViewById<TextView>(R.id.tv_status_title)?.text.toString()
                val percentage = view.findViewById<TextView>(R.id.tv_percentage)?.text.toString()
                val adherenceMsg = view.findViewById<TextView>(R.id.tv_adherence_message)?.text.toString()
                val insight1 = view.findViewById<TextView>(R.id.tv_insight1)?.text.toString()
                val insight2 = view.findViewById<TextView>(R.id.tv_insight2)?.text.toString()

                // 2. Create a new PDF document with A4 dimensions (595x842)
                val document = PdfDocument()
                val pageWidth = 595
                val pageHeight = 842
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // Set white background for the PDF
                canvas.drawColor(Color.WHITE)

                // 3. Prepare drawing paints (Fonts, Colors, Styles)
                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 26f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    textAlign = Paint.Align.CENTER
                }

                val sectionPaint = Paint().apply {
                    color = Color.rgb(46, 125, 50) // Professional Green Color
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

                // 4. Draw content on the Canvas
                var currentY = 60f
                val marginX = 50f
                val contentWidth = pageWidth - (marginX * 2).toInt()

                // Header section
                canvas.drawText(getString(R.string.app_name), pageWidth / 2f, currentY, titlePaint)
                currentY += 30f

                titlePaint.textSize = 16f
                titlePaint.color = Color.GRAY
                canvas.drawText(getString(R.string.pdf_report_title), pageWidth / 2f, currentY, titlePaint)
                currentY += 40f

                val sdf = SimpleDateFormat("MMMM dd, yyyy - HH:mm", Locale.getDefault())
                val currentDate = sdf.format(Date())
                canvas.drawText(getString(R.string.pdf_generated_on, currentDate), marginX, currentY, textPaint)
                currentY += 20f

                // Divider line
                canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
                currentY += 40f

                // Section 1: Current Health Status
                canvas.drawText(getString(R.string.pdf_section_health_status), marginX, currentY, sectionPaint)
                currentY += 30f
                canvas.drawText(statusTitle, marginX + 15f, currentY, textPaint)
                currentY += 40f

                // Divider line
                canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
                currentY += 40f

                // Section 2: Medication Adherence
                canvas.drawText(getString(R.string.pdf_section_adherence), marginX, currentY, sectionPaint)
                currentY += 30f

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(getString(R.string.pdf_adherence_rate, percentage), marginX + 15f, currentY, textPaint)
                currentY += 25f

                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

                // Use StaticLayout to wrap long text into multiple lines
                val adherenceLayout = StaticLayout.Builder.obtain(adherenceMsg, 0, adherenceMsg.length, textPaint, contentWidth - 15)
                    .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .build()
                canvas.save()
                canvas.translate(marginX + 15f, currentY)
                adherenceLayout.draw(canvas)
                canvas.restore()
                currentY += adherenceLayout.height + 40f

                // Divider line
                canvas.drawLine(marginX, currentY, pageWidth - marginX, currentY, linePaint)
                currentY += 40f

                // Section 3: AI Insights & Recommendations
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

                // Footer section
                val footerPaint = Paint().apply {
                    color = Color.LTGRAY
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(getString(R.string.pdf_footer, getString(R.string.app_name)), pageWidth / 2f, pageHeight - 40f, footerPaint)

                // Finish the PDF page
                document.finishPage(page)

                // 5. Save the generated PDF file safely to the device's Downloads folder
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
                        Toast.makeText(requireContext(), getString(R.string.pdf_saved_success), Toast.LENGTH_LONG).show()
                    }
                } else {
                    // Fallback for Android 9 (Pie) and below
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { outputStream ->
                        document.writeTo(outputStream)
                    }
                    Toast.makeText(requireContext(), getString(R.string.pdf_saved_success), Toast.LENGTH_LONG).show()
                }

                document.close()

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(requireContext(), getString(R.string.pdf_save_failed, e.message), Toast.LENGTH_SHORT).show()
            }
        }
    }
}