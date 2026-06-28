package com.example.smarthealthreminder.features.data.repository

import com.example.smarthealthreminder.features.data.local.dao.ReportDao
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity
import kotlinx.coroutines.flow.Flow

class ReportRepository(private val reportDao: ReportDao) {

    // قراءة كل التقارير
    val allReports: Flow<List<ReportEntity>> = reportDao.getAllReports()

    // إضافة تقرير
    suspend fun insertReport(report: ReportEntity) {
        reportDao.insertReport(report)
    }
}