package com.example.smarthealthreminder.features.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object RecurrenceHelper {

    const val NONE = "None"
    const val DAILY = "Daily"
    const val WEEKLY = "Weekly"
    const val MONTHLY = "Monthly"

    val OPTIONS = arrayOf(NONE, DAILY, WEEKLY, MONTHLY)

    fun isRecurring(recurrenceType: String?): Boolean {
        return !recurrenceType.isNullOrBlank() && !recurrenceType.equals(NONE, ignoreCase = true)
    }

    fun parseDateTimeMillis(date: String?, time: String?): Long? {
        if (date.isNullOrBlank() || time.isNullOrBlank()) return null
        return try {
            val dateParts = date.split("-")
            val timeParts = time.split(":")
            if (dateParts.size < 3 || timeParts.size < 2) return null

            Calendar.getInstance().apply {
                set(Calendar.YEAR, dateParts[0].toInt())
                set(Calendar.MONTH, dateParts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, dateParts[2].toInt())
                set(Calendar.HOUR_OF_DAY, timeParts[0].toInt())
                set(Calendar.MINUTE, timeParts[1].toInt())
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    fun formatDate(millis: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        return String.format(
            Locale.getDefault(),
            "%04d-%02d-%02d",
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun getTodayString(): String = formatDate(System.currentTimeMillis())

    fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        return try {
            val sdf1 = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }
            val parsed = sdf1.parse(dateStr) ?: return dateStr
            formatDate(parsed.time)
        } catch (e: Exception) {
            try {
                val sdf2 = SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).apply { isLenient = false }
                val parsed = sdf2.parse(dateStr) ?: return dateStr
                formatDate(parsed.time)
            } catch (e2: Exception) {
                dateStr
            }
        }
    }

    fun computeNextTriggerMillis(
        date: String?,
        time: String?,
        recurrenceType: String?,
        fromMillis: Long = System.currentTimeMillis()
    ): Long? {
        val baseMillis = parseDateTimeMillis(date, time) ?: return null
        if (!isRecurring(recurrenceType)) {
            return if (baseMillis > fromMillis) baseMillis else null
        }

        var trigger = baseMillis
        val cal = Calendar.getInstance()
        var safety = 0
        while (trigger <= fromMillis && safety < 400) {
            cal.timeInMillis = trigger
            when (recurrenceType) {
                DAILY -> cal.add(Calendar.DAY_OF_YEAR, 1)
                WEEKLY -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                MONTHLY -> cal.add(Calendar.MONTH, 1)
                else -> return null
            }
            trigger = cal.timeInMillis
            safety++
        }
        return if (trigger > fromMillis) trigger else null
    }

    fun isDueOnDate(
        reminderDate: String?,
        recurrenceType: String?,
        isRecurring: Boolean,
        targetDate: String
    ): Boolean {
        val normalizedTarget = normalizeDate(targetDate)
        if (!isRecurring || !isRecurring(recurrenceType)) {
            return normalizeDate(reminderDate) == normalizedTarget
        }

        val anchorMillis = parseDateTimeMillis(normalizeDate(reminderDate), "00:00") ?: return false
        val targetMillis = parseDateTimeMillis(normalizedTarget, "00:00") ?: return false
        if (targetMillis < anchorMillis) return false

        val anchor = Calendar.getInstance().apply { timeInMillis = anchorMillis }
        val target = Calendar.getInstance().apply { timeInMillis = targetMillis }

        return when (recurrenceType) {
            DAILY -> true
            WEEKLY -> anchor.get(Calendar.DAY_OF_WEEK) == target.get(Calendar.DAY_OF_WEEK)
            MONTHLY -> anchor.get(Calendar.DAY_OF_MONTH) == target.get(Calendar.DAY_OF_MONTH)
            else -> normalizeDate(reminderDate) == normalizedTarget
        }
    }
}
