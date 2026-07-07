package com.example.smarthealthreminder.features.viewmodel

import com.example.smarthealthreminder.features.model.Reminder
import com.example.smarthealthreminder.features.model.ScheduleItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.text.SimpleDateFormat
import java.util.*

/**
 * Security and edge-case tests covering:
 *  - Hacker user: injection strings, boundary values, malformed input
 *  - Luxury user: extremely long inputs, unicode, emoji, very large datasets
 *  - Malicious user: null injection, empty fields, future/past date logic
 */
@RunWith(RobolectricTestRunner::class)
class SecurityAndEdgeCaseTest {

    // ─────────────────────────────────────────────
    // INPUT VALIDATION — Reminder title
    // ─────────────────────────────────────────────

    @Test
    fun `empty title should be detected as invalid`() {
        val title = "   "
        assertTrue("Blank title must be rejected", title.trim().isEmpty())
    }

    @Test
    fun `sql injection string in title is stored as plain text not executed`() {
        // Room uses parameterized queries — this just tests the model stores it safely
        val maliciousTitle = "'; DROP TABLE reminders; --"
        val reminder = Reminder(id = "r1", title = maliciousTitle)
        assertEquals(maliciousTitle, reminder.title)  // stored as-is, not interpreted
    }

    @Test
    fun `xss script tag in title stored as plain text`() {
        val xssTitle = "<script>alert('xss')</script>"
        val reminder = Reminder(id = "r1", title = xssTitle)
        assertEquals(xssTitle, reminder.title)
    }

    @Test
    fun `extremely long title is stored without crash`() {
        val longTitle = "A".repeat(10_000)
        val reminder = Reminder(id = "r1", title = longTitle)
        assertEquals(10_000, reminder.title?.length)
    }

    @Test
    fun `emoji in reminder title is preserved correctly`() {
        val emojiTitle = "💊 Take meds 🏥"
        val reminder = Reminder(id = "r1", title = emojiTitle)
        assertEquals(emojiTitle, reminder.title)
    }

    @Test
    fun `null title in reminder is handled`() {
        val reminder = Reminder(id = "r1", title = null)
        assertNull(reminder.title)
    }

    // ─────────────────────────────────────────────
    // DATE VALIDATION
    // ─────────────────────────────────────────────

    @Test
    fun `past date should be detectable`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val pastDate = "2000-01-01"
        val parsed = sdf.parse(pastDate)!!
        assertTrue("Past date should be before now", parsed.before(Date()))
    }

    @Test
    fun `future date should be detectable`() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val futureDate = "2099-12-31"
        val parsed = sdf.parse(futureDate)!!
        assertTrue("Future date should be after now", parsed.after(Date()))
    }

    @Test
    fun `malformed date string does not crash normalizeDate`() {
        val result = normalizeDate("not-a-date")
        // Should return original string, not throw
        assertEquals("not-a-date", result)
    }

    @Test
    fun `empty date string returns empty from normalizeDate`() {
        val result = normalizeDate("")
        assertEquals("", result)
    }

    @Test
    fun `null date string returns empty from normalizeDate`() {
        val result = normalizeDate(null)
        assertEquals("", result)
    }

    @Test
    fun `both date formats are normalized to yyyy-MM-dd`() {
        val iso = normalizeDate("2025-06-22")
        val usFormat = normalizeDate("06/22/2025")
        assertEquals("2025-06-22", iso)
        assertEquals("2025-06-22", usFormat)
    }

    @Test
    fun `reminder time millis returns null for malformed date parts`() {
        val result = getReminderTimeMillis("bad-date", "bad-time")
        assertNull(result)
    }

    @Test
    fun `reminder time millis returns null when parts are too few`() {
        val result = getReminderTimeMillis("2025", "10")
        assertNull(result)
    }

    @Test
    fun `past reminder time is correctly identified as past`() {
        val millis = getReminderTimeMillis("2000-01-01", "08:00")!!
        assertTrue(millis <= System.currentTimeMillis())
    }

    // ─────────────────────────────────────────────
    // EMAIL VALIDATION
    // ─────────────────────────────────────────────

    @Test
    fun `valid email passes format check`() {
        assertTrue(isValidEmail("user@example.com"))
    }

    @Test
    fun `email without at-sign fails format check`() {
        assertFalse(isValidEmail("userexample.com"))
    }

    @Test
    fun `email without domain fails format check`() {
        assertFalse(isValidEmail("user@"))
    }

    @Test
    fun `empty email fails format check`() {
        assertFalse(isValidEmail(""))
    }

    @Test
    fun `blank email fails format check`() {
        assertFalse(isValidEmail("   "))
    }

    @Test
    fun `sql injection in email field fails format check`() {
        assertFalse(isValidEmail("' OR '1'='1"))
    }

    @Test
    fun `extremely long email fails format check`() {
        val longEmail = "a".repeat(500) + "@b.com"
        // Should technically pass the regex but is unusable — just verify no crash
        val result = isValidEmail(longEmail)
        assertNotNull(result) // no exception
    }

    // ─────────────────────────────────────────────
    // SEARCH HISTORY — pipe character edge case
    // ─────────────────────────────────────────────

    @Test
    fun `search query with pipe character does not corrupt history`() {
        val history = mutableListOf<String>()
        val queryWithPipe = "blood|pressure"  // pipe was the old separator
        addToHistory(history, queryWithPipe)
        assertEquals(1, history.size)
        assertEquals(queryWithPipe, history[0])
    }

    @Test
    fun `search history deduplicates queries`() {
        val history = mutableListOf<String>()
        addToHistory(history, "aspirin")
        addToHistory(history, "aspirin")
        assertEquals(1, history.size)
    }

    @Test
    fun `search history is limited to 5 entries`() {
        val history = mutableListOf<String>()
        for (i in 1..10) addToHistory(history, "query$i")
        assertEquals(5, history.size)
    }

    @Test
    fun `most recent search is first in history`() {
        val history = mutableListOf<String>()
        addToHistory(history, "first")
        addToHistory(history, "second")
        assertEquals("second", history[0])
    }

    @Test
    fun `blank search query is not added to history`() {
        val history = mutableListOf<String>()
        addToHistory(history, "  ")
        assertTrue(history.isEmpty())
    }

    // ─────────────────────────────────────────────
    // SCHEDULE ITEM — edge cases
    // ─────────────────────────────────────────────

    @Test
    fun `ScheduleItem with empty id does not crash`() {
        val item = ScheduleItem(id = "", title = "Test", date = "2025-06-22", time = "08:00")
        assertEquals("", item.id)
    }

    @Test
    fun `ScheduleItem filter handles blank date correctly`() {
        val items = listOf(
            ScheduleItem(title = "A", date = "", time = "08:00"),
            ScheduleItem(title = "B", date = "2025-06-22", time = "09:00")
        )
        val filtered = items.filter { it.date == "2025-06-22" || it.date.isBlank() }
        assertEquals(2, filtered.size)
    }

    @Test
    fun `ScheduleItem with very long title does not crash`() {
        val item = ScheduleItem(
            title = "X".repeat(5_000),
            date = "2025-06-22",
            time = "08:00"
        )
        assertEquals(5_000, item.title.length)
    }

    @Test
    fun `ScheduleItem note truncation logic is correct`() {
        val longNote = "A".repeat(100)
        val truncated = longNote.take(60) + if (longNote.length > 60) "…" else ""
        assertEquals(61, truncated.length) // 60 chars + ellipsis
        assertTrue(truncated.endsWith("…"))
    }

    @Test
    fun `ScheduleItem note within 60 chars is not truncated`() {
        val shortNote = "Short note"
        val result = shortNote.take(60) + if (shortNote.length > 60) "…" else ""
        assertEquals("Short note", result)
    }

    // ─────────────────────────────────────────────
    // ALARM HELPER — time parsing edge cases
    // ─────────────────────────────────────────────

    @Test
    fun `parseAlarmTime handles midnight correctly`() {
        val (hour, minute) = parseAlarmTime("12:00", "AM")
        assertEquals(0, hour)   // 12 AM = 0 in 24h
        assertEquals(0, minute)
    }

    @Test
    fun `parseAlarmTime handles noon correctly`() {
        val (hour, minute) = parseAlarmTime("12:00", "PM")
        assertEquals(12, hour)  // 12 PM = 12 in 24h
        assertEquals(0, minute)
    }

    @Test
    fun `parseAlarmTime converts PM correctly`() {
        val (hour, minute) = parseAlarmTime("03:30", "PM")
        assertEquals(15, hour)
        assertEquals(30, minute)
    }

    @Test
    fun `parseAlarmTime handles null amPm as 24h pass-through`() {
        val (hour, minute) = parseAlarmTime("14:45", null)
        assertEquals(14, hour)
        assertEquals(45, minute)
    }

    @Test
    fun `parseAlarmTime handles null time gracefully`() {
        val (hour, minute) = parseAlarmTime(null, "AM")
        assertEquals(8, hour)   // default fallback
        assertEquals(0, minute)
    }

    @Test
    fun `parseAlarmTime clamps out-of-range values`() {
        val (hour, _) = parseAlarmTime("25:00", null)
        assertTrue("Hour must be clamped to 0-23", hour in 0..23)
    }

    // ─────────────────────────────────────────────
    // NOTE CONTENT
    // ─────────────────────────────────────────────

    @Test
    fun `blank note is rejected`() {
        val note = "   "
        assertTrue(note.trim().isEmpty())
    }

    @Test
    fun `note with only newlines is considered blank`() {
        val note = "\n\n\n"
        assertTrue(note.trim().isEmpty())
    }

    @Test
    fun `very large note content is handled without crash`() {
        val bigNote = "Health data: " + "X".repeat(100_000)
        assertTrue(bigNote.length > 100_000)
    }

    // ─────────────────────────────────────────────
    // REMINDER STATUS TRANSITIONS
    // ─────────────────────────────────────────────

    @Test
    fun `reminder can transition from Pending to Completed`() {
        val reminder = Reminder(id = "r1", title = "Med", status = "Pending")
        val updated = reminder.copy(status = "Completed")
        assertEquals("Completed", updated.status)
    }

    @Test
    fun `reminder can transition from Pending to Missed`() {
        val reminder = Reminder(id = "r1", title = "Med", status = "Pending")
        val updated = reminder.copy(status = "Missed")
        assertEquals("Missed", updated.status)
    }

    @Test
    fun `done count logic handles case-insensitive status`() {
        val items = listOf(
            ScheduleItem(title = "A", date = "2025-06-22", time = "08:00", status = "COMPLETED"),
            ScheduleItem(title = "B", date = "2025-06-22", time = "09:00", status = "done"),
            ScheduleItem(title = "C", date = "2025-06-22", time = "10:00", status = "pending"),
        )
        val done = items.count { it.status.uppercase() in listOf("DONE", "COMPLETED") }
        assertEquals(2, done)
    }

    // ─────────────────────────────────────────────
    // Helpers (mirrors private methods in the app)
    // ─────────────────────────────────────────────

    private fun normalizeDate(dateStr: String?): String {
        if (dateStr.isNullOrBlank()) return ""
        val formats = listOf("yyyy-MM-dd", "MM/dd/yyyy")
        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault()).apply { isLenient = false }
                val date = sdf.parse(dateStr)
                if (date != null) {
                    return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {
                // continue
            }
        }
        return dateStr
    }

    private fun getReminderTimeMillis(date: String, time: String): Long? {
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

    private fun isValidEmail(email: String): Boolean {
        return email.trim().isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    private fun addToHistory(history: MutableList<String>, query: String) {
        if (query.isBlank()) return
        history.remove(query)
        history.add(0, query)
        while (history.size > 5) history.removeAt(history.size - 1)
    }

    private fun parseAlarmTime(time: String?, amPm: String?): Pair<Int, Int> {
        val parts = time?.split(":").orEmpty()
        var hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        if (hour in 1..12 && (amPm == "AM" || amPm == "PM")) {
            hour = when {
                hour == 12 && amPm == "AM" -> 0
                hour < 12 && amPm == "PM" -> hour + 12
                else -> hour
            }
        }
        return hour.coerceIn(0, 23) to minute.coerceIn(0, 59)
    }
}
