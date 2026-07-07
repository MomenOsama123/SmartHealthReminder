package com.example.smarthealthreminder.features.util

import android.content.Context
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.data.local.dao.ReminderDao
import com.example.smarthealthreminder.features.data.local.entity.ReportEntity

class ReportGenerator(private val context: Context, private val reminderDao: ReminderDao) {

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
            title = context.getString(R.string.report_title_weekly),
            adherencePercentage = percentage,
            missedDoses = missed,
            symptomsOverview = context.getString(R.string.report_symptoms_format, total, completed, missed),
            aiInsight1 = when {
                percentage >= 90 -> context.getString(R.string.insight_excellent)
                percentage >= 75 -> context.getString(R.string.insight_good)
                else -> context.getString(R.string.insight_low_adherence)
            },
            aiInsight2 = if (missed > 0) context.getString(R.string.insight_missed_busy) else context.getString(R.string.insight_perfect_score),
            date = today
        )
    }
}
