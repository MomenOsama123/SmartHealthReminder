package com.example.smarthealthreminder.features.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.smarthealthreminder.features.data.local.dao.ReminderDao
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import com.example.smarthealthreminder.features.data.repository.ReportRepository
import com.example.smarthealthreminder.features.util.ReportGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// عدل الـ Constructor عشان يستقبل الـ reminderDao والـ application
class ReportViewModel(
    application: Application,
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao
) : AndroidViewModel(application) {

    // ضيف السطر ده عشان الشاشة تقدر تلاقي البيانات وتراقبها
    val allReports: Flow<List<ReportEntity>> = repository.allReports

    fun generateRealReport() {
        viewModelScope.launch(Dispatchers.IO) {
            val generator = ReportGenerator(getApplication(), reminderDao)
            val realReport = generator.generateWeeklyReport()
            repository.insertReport(realReport)
        }
    }
}

// الكلاس ده ضروري عشان نعرف ننشئ الـ ViewModel واحنا بنباصيله الـ Repository
class ReportViewModelFactory(
    private val application: Application,
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao // ضفنا ده هنا كمان
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReportViewModel(application, repository, reminderDao) as T // مررناه هنا
    }
}
