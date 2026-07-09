package com.example.smarthealthreminder.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smarthealthreminder.features.data.local.dao.ReminderDao
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.example.smarthealthreminder.features.util.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class ReportViewModel(
    application: Application,
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao
) : AndroidViewModel(application) {

    private var isGenerating = false

    // Expose all reports from database as a Flow to observe updates
    val allReports: Flow<List<ReportEntity>> = repository.allReports

    // Core function to generate a new report using the generator utility
    fun generateRealReport() {
        if (isGenerating) return
        isGenerating = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val generator = ReportGenerator(getApplication(), reminderDao)
                val realReport = generator.generateWeeklyReport()
                repository.insertReport(realReport)
            } finally {
                isGenerating = false
            }
        }
    }

    // =========================================================================
    // الدالة الجديدة الخاصة بـ Weekly Reminders اللي الشاشة بتطلبها
    // =========================================================================
    fun getRecentReminders(sevenDaysAgo: Long): Flow<List<ReminderEntity>> {
        // بنسحب كل التذكيرات من الـ DAO، وبنفلترها هنا (Mapping)
        // عشان نحول حقل date اللي هو String لـ Milliseconds ونقارنه بآخر 7 أيام
        return reminderDao.getAllReminders().map { remindersList ->
            remindersList.filter { reminder ->
                val reminderTime = parseDateToMillis(reminder.date)
                reminderTime >= sevenDaysAgo // لو التذكير تم في آخر 7 أيام، هيرجع للشاشة
            }
        }
    }

    // دالة مساعدة لتحويل التاريخ النصي (String) لـ Milliseconds
    private fun parseDateToMillis(dateString: String?): Long {
        if (dateString.isNullOrEmpty()) return 0L

        val formats = arrayOf("dd/MM/yyyy", "yyyy-MM-dd", "MM/dd/yyyy", "yyyy/MM/dd")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                val date = sdf.parse(dateString)
                if (date != null) {
                    return date.time
                }
            } catch (e: Exception) {
                // Ignore format mismatch and try the next one
            }
        }
        return 0L // لو فشل التحويل (تاريخ فاضي أو غلط)
    }
}

// Factory class required to pass custom dependencies into the ViewModel
class ReportViewModelFactory(
    private val application: Application,
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReportViewModel(application, repository, reminderDao) as T
    }
}