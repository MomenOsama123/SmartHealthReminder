package com.example.smarthealthreminder.features.fragment

import com.example.smarthealthreminder.features.model.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for ScheduleItem model and type constants.
 * Covers schedule, reminder, alarm, report, and note item types used
 * by ScheduleFragment, DayDetailsActivity, and TodayPlanActivity.
 */
class ScheduleItemTest {

    // ───── Item type constants ─────

    @Test
    fun `TYPE_REMINDER constant is correct`() {
        assertEquals("reminder", ScheduleItem.TYPE_REMINDER)
    }

    @Test
    fun `TYPE_ALARM constant is correct`() {
        assertEquals("alarm", ScheduleItem.TYPE_ALARM)
    }

    @Test
    fun `TYPE_SCHEDULE_ENTRY constant is correct`() {
        assertEquals("schedule_entry", ScheduleItem.TYPE_SCHEDULE_ENTRY)
    }

    @Test
    fun `TYPE_REPORT constant is correct`() {
        assertEquals("report", ScheduleItem.TYPE_REPORT)
    }

    @Test
    fun `TYPE_NOTE constant is correct`() {
        assertEquals("note", ScheduleItem.TYPE_NOTE)
    }

    // ───── Default values ─────

    @Test
    fun `ScheduleItem defaults itemType to TYPE_REMINDER`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertEquals(ScheduleItem.TYPE_REMINDER, item.itemType)
    }

    @Test
    fun `ScheduleItem defaults category to General`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertEquals("General", item.category)
    }

    @Test
    fun `ScheduleItem defaults priority to NORMAL`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertEquals("NORMAL", item.priority)
    }

    @Test
    fun `ScheduleItem defaults status to Pending`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertEquals("Pending", item.status)
    }

    @Test
    fun `ScheduleItem defaults isAlarm to false`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertFalse(item.isAlarm)
    }

    @Test
    fun `ScheduleItem defaults earlyNotification to false`() {
        val item = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00")
        assertFalse(item.earlyNotification)
    }

    // ───── Alarm item ─────

    @Test
    fun `ScheduleItem alarm item has correct type and isAlarm true`() {
        val item = ScheduleItem(
            id = "a1",
            title = "Morning Alarm",
            date = "daily",
            time = "08:00 AM",
            category = "Alarm",
            isAlarm = true,
            itemType = ScheduleItem.TYPE_ALARM
        )
        assertEquals(ScheduleItem.TYPE_ALARM, item.itemType)
        assertTrue(item.isAlarm)
    }

    // ───── Schedule entry item ─────

    @Test
    fun `ScheduleItem schedule entry has correct type`() {
        val item = ScheduleItem(
            id = "se1",
            title = "Doctor Visit",
            date = "2025-06-22",
            time = "10:00",
            category = "Appointment",
            itemType = ScheduleItem.TYPE_SCHEDULE_ENTRY
        )
        assertEquals(ScheduleItem.TYPE_SCHEDULE_ENTRY, item.itemType)
    }

    // ───── Report item ─────

    @Test
    fun `ScheduleItem report item has Completed status`() {
        val item = ScheduleItem(
            id = "rep1",
            title = "Weekly Report",
            date = "2025-06-22",
            time = "Report",
            category = "Report",
            status = "Completed",
            itemType = ScheduleItem.TYPE_REPORT
        )
        assertEquals("Completed", item.status)
        assertEquals(ScheduleItem.TYPE_REPORT, item.itemType)
    }

    // ───── Note item ─────

    @Test
    fun `ScheduleItem note item has correct type`() {
        val item = ScheduleItem(
            id = "2025-06-22",
            title = "Note for today",
            date = "2025-06-22",
            time = "Note",
            category = "Note",
            itemType = ScheduleItem.TYPE_NOTE
        )
        assertEquals(ScheduleItem.TYPE_NOTE, item.itemType)
    }

    // ───── Equality & copy ─────

    @Test
    fun `ScheduleItem equality based on all fields`() {
        val i1 = ScheduleItem(id = "1", title = "A", date = "2025-06-22", time = "08:00")
        val i2 = ScheduleItem(id = "1", title = "A", date = "2025-06-22", time = "08:00")
        assertEquals(i1, i2)
    }

    @Test
    fun `ScheduleItem copy updates field correctly`() {
        val original = ScheduleItem(title = "Test", date = "2025-06-22", time = "08:00", status = "Pending")
        val updated = original.copy(status = "Completed")
        assertEquals("Completed", updated.status)
        assertEquals("Test", updated.title)
    }

    // ───── Date filtering logic (mirrors ScheduleFragment.applyFilter) ─────

    @Test
    fun `items with matching date are included in filter`() {
        val today = "2025-06-22"
        val items = listOf(
            ScheduleItem(id = "1", title = "A", date = today, time = "08:00"),
            ScheduleItem(id = "2", title = "B", date = "2025-06-23", time = "09:00"),
            ScheduleItem(id = "3", title = "C", date = "daily", time = "10:00"),
            ScheduleItem(id = "4", title = "D", date = "", time = "11:00")
        )

        val filtered = items.filter {
            it.date == today || it.date == "daily" || it.date.isBlank()
        }

        assertEquals(3, filtered.size)
        assertTrue(filtered.any { it.id == "1" })
        assertTrue(filtered.any { it.id == "3" })
        assertTrue(filtered.any { it.id == "4" })
        assertFalse(filtered.any { it.id == "2" })
    }

    @Test
    fun `pending and done count logic from ScheduleFragment`() {
        val items = listOf(
            ScheduleItem(title = "A", date = "2025-06-22", time = "08:00", status = "Pending"),
            ScheduleItem(title = "B", date = "2025-06-22", time = "09:00", status = "Completed"),
            ScheduleItem(title = "C", date = "2025-06-22", time = "10:00", status = "DONE"),
            ScheduleItem(title = "D", date = "2025-06-22", time = "11:00", status = "PENDING"),
        )

        val pending = items.count { it.status.uppercase() == "PENDING" }
        val done = items.count { it.status.uppercase() in listOf("DONE", "COMPLETED") }

        assertEquals(2, pending)
        assertEquals(2, done)
    }
}
