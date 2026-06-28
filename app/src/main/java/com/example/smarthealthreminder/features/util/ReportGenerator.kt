package com.example.smarthealthreminder.features.util

import com.example.smarthealthreminder.features.data.local.dao.ReminderDao
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity

class ReportGenerator(private val reminderDao: ReminderDao) {

    suspend fun generateWeeklyReport(): ReportEntity {
        // 1. جلب البيانات
        val completed = reminderDao.getCompletedCountRaw()
        val missed = reminderDao.getMissedCountRaw()
        val total = completed + missed

        // 2. الحسابات
        val percentage = if (total > 0) (completed * 100) / total else 0

        // 3. إرجاع النتيجة (إرجاع التقرير)
        return ReportEntity(
            id = "report_${System.currentTimeMillis()}",
            title = "Weekly Health Summary",
            adherencePercentage = percentage,
            missedDoses = missed,
            symptomsOverview = "Analysis based on $total recorded medication events.",
            aiInsight1 = if (percentage >= 80) "Excellent consistency!" else "You can do better next week.",
            aiInsight2 = "Keep tracking your medication daily.",
            date = "2026-06-28" // يمكنك جعل التاريخ ديناميكي لاحقاً
        )
    }
}