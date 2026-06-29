package com.example.smarthealthreminder.features.viewmodel

import com.example.smarthealthreminder.features.data.local.entity.ReminderEntity
import com.example.smarthealthreminder.features.model.Reminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Reminder domain model and ReminderEntity data class.
 * Covers: construction, defaults, equality, Parcelable CREATOR, copy.
 */
class ReminderModelTest {

    // ───── Reminder domain model ─────

    @Test
    fun `Reminder default constructor sets status to Pending`() {
        val reminder = Reminder()
        assertEquals("Pending", reminder.status)
    }

    @Test
    fun `Reminder convenience constructor sets all fields`() {
        val reminder = Reminder(
            title = "Take Aspirin",
            description = "After dinner",
            category = "Medicine",
            date = "2025-06-22",
            time = "20:00",
            priority = "High"
        )
        assertEquals("Take Aspirin", reminder.title)
        assertEquals("After dinner", reminder.description)
        assertEquals("Medicine", reminder.category)
        assertEquals("2025-06-22", reminder.date)
        assertEquals("20:00", reminder.time)
        assertEquals("High", reminder.priority)
        assertEquals("Pending", reminder.status)
    }

    @Test
    fun `Reminder isRecurring defaults to false`() {
        val reminder = Reminder()
        assertFalse(reminder.isRecurring)
    }

    @Test
    fun `Reminder vibrationEnabled defaults to false`() {
        val reminder = Reminder()
        assertFalse(reminder.vibrationEnabled)
    }

    @Test
    fun `Reminder earlyNotificationMinutes defaults to zero`() {
        val reminder = Reminder()
        assertEquals(0, reminder.earlyNotificationMinutes)
    }

    @Test
    fun `Reminder data class equality works`() {
        val r1 = Reminder(id = "r1", title = "Med", date = "2025-06-22")
        val r2 = Reminder(id = "r1", title = "Med", date = "2025-06-22")
        assertEquals(r1, r2)
    }

    @Test
    fun `Reminder with different ids are not equal`() {
        val r1 = Reminder(id = "r1", title = "Med")
        val r2 = Reminder(id = "r2", title = "Med")
        assertTrue(r1 != r2)
    }

    @Test
    fun `Reminder copy preserves unchanged fields`() {
        val original = Reminder(id = "r1", title = "Med", status = "Pending")
        val updated = original.copy(status = "Completed")
        assertEquals("Completed", updated.status)
        assertEquals("r1", updated.id)
        assertEquals("Med", updated.title)
    }

    @Test
    fun `Reminder CREATOR newArray returns correct size`() {
        val array = Reminder.newArray(3)
        assertEquals(3, array.size)
    }

    // ───── ReminderEntity ─────

    @Test
    fun `ReminderEntity status defaults to Pending`() {
        val entity = ReminderEntity(id = "e1", title = "Test Reminder")
        assertEquals("Pending", entity.status)
    }

    @Test
    fun `ReminderEntity optional fields default to null`() {
        val entity = ReminderEntity(id = "e1", title = "Test")
        assertNull(entity.description)
        assertNull(entity.category)
        assertNull(entity.date)
        assertNull(entity.time)
        assertNull(entity.priority)
    }

    @Test
    fun `ReminderEntity isRecurring defaults to false`() {
        val entity = ReminderEntity(id = "e1", title = "Test")
        assertFalse(entity.isRecurring)
    }

    @Test
    fun `ReminderEntity earlyNotification defaults to false`() {
        val entity = ReminderEntity(id = "e1", title = "Test")
        assertFalse(entity.earlyNotification)
    }

    @Test
    fun `ReminderEntity copy with updated status works`() {
        val entity = ReminderEntity(id = "e1", title = "Test", status = "Pending")
        val updated = entity.copy(status = "Completed")
        assertEquals("Completed", updated.status)
        assertEquals("e1", updated.id)
    }

    @Test
    fun `ReminderEntity equality based on all fields`() {
        val e1 = ReminderEntity(id = "e1", title = "Test", date = "2025-06-22")
        val e2 = ReminderEntity(id = "e1", title = "Test", date = "2025-06-22")
        assertEquals(e1, e2)
    }

    // ───── Entity to domain model mapping ─────

    @Test
    fun `mapping ReminderEntity to Reminder preserves all fields`() {
        val entity = ReminderEntity(
            id = "r1",
            title = "Morning Pill",
            description = "With food",
            category = "Medicine",
            date = "2025-06-22",
            time = "08:00",
            priority = "High",
            status = "Pending",
            isRecurring = true,
            recurrenceType = "Daily",
            vibrationEnabled = true,
            earlyNotification = true,
            earlyNotificationMinutes = 5
        )
        val model = Reminder(
            id = entity.id,
            title = entity.title,
            description = entity.description,
            category = entity.category,
            date = entity.date,
            time = entity.time,
            priority = entity.priority,
            status = entity.status,
            isRecurring = entity.isRecurring,
            recurrenceType = entity.recurrenceType,
            vibrationEnabled = entity.vibrationEnabled,
            earlyNotification = entity.earlyNotification,
            earlyNotificationMinutes = entity.earlyNotificationMinutes
        )
        assertEquals("r1", model.id)
        assertEquals("Morning Pill", model.title)
        assertEquals("With food", model.description)
        assertEquals("Medicine", model.category)
        assertEquals("2025-06-22", model.date)
        assertEquals("08:00", model.time)
        assertEquals("High", model.priority)
        assertEquals("Pending", model.status)
        assertTrue(model.isRecurring)
        assertEquals("Daily", model.recurrenceType)
        assertTrue(model.vibrationEnabled)
        assertTrue(model.earlyNotification)
        assertEquals(5, model.earlyNotificationMinutes)
    }
}
