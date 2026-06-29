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
        
        val today = RecurrenceHelper.getTodayString()

        // 3. إرجاع النتيجة (إرجاع التقرير)
        return ReportEntity(
            id = "report_${System.currentTimeMillis()}",
            title = "Weekly Health Summary",
            adherencePercentage = percentage,
            missedDoses = missed,
            symptomsOverview = "Analysis based on $total recorded medication events ($completed taken, $missed missed).",
            aiInsight1 = when {
                percentage >= 90 -> "Excellent consistency! Keep up the great work."
                percentage >= 75 -> "Good job, but try to minimize missing doses."
                else -> "Your adherence is low. Setting extra reminders might help."
            },
            aiInsight2 = if (missed > 0) "Most missed doses occur when you're busy. Try to plan ahead." else "You haven't missed any doses lately. Perfect score!",
            date = today
        )
    }
}