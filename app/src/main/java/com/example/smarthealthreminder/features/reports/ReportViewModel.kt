package com.example.smarthealthreminder.features.reports

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

// عدل الـ Constructor عشان يستقبل الـ reminderDao
class ReportViewModel(
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao
) : ViewModel() {

    // ضيف السطر ده عشان الشاشة تقدر تلاقي البيانات وتراقبها
    val allReports: Flow<List<ReportEntity>> = repository.allReports

    fun generateRealReport() {
        viewModelScope.launch(Dispatchers.IO) {
            val generator = ReportGenerator(reminderDao)
            val realReport = generator.generateWeeklyReport()
            repository.insertReport(realReport)
        }
    }
}

// الكلاس ده ضروري عشان نعرف ننشئ الـ ViewModel واحنا بنباصيله الـ Repository
class ReportViewModelFactory(
    private val repository: ReportRepository,
    private val reminderDao: ReminderDao // ضفنا ده هنا كمان
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ReportViewModel(repository, reminderDao) as T // مررناه هنا
    }
}