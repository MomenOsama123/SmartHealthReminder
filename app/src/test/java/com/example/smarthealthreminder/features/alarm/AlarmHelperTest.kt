package com.example.smarthealthreminder.features.alarm

import android.app.AlarmManager
import android.content.Context
import android.os.Build
import com.example.smarthealthreminder.alarm.AlarmHelper
import com.example.smarthealthreminder.features.model.Alarm
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import junit.framework.TestCase.assertEquals
import java.util.Calendar

class AlarmHelperTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var alarmHelper: AlarmHelper

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)
        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
    }

    // ───── canScheduleExactAlarm ─────

    @Test
    fun `canScheduleExactAlarm returns true when SDK below S`() {
        // On test JVM, Build.VERSION.SDK_INT is 0, which is < S (31), so should return true
        alarmHelper = AlarmHelper(context)
        assertTrue(alarmHelper.canScheduleExactAlarm())
    }

    // ───── scheduleAlarm ─────

    @Test
    fun `scheduleAlarm returns false when canScheduleExactAlarm is false`() {
        // We need to spy on AlarmHelper to mock canScheduleExactAlarm
        alarmHelper = spyk(AlarmHelper(context))
        every { alarmHelper.canScheduleExactAlarm() } returns false

        val alarm = Alarm(
            id = "a1", label = "Test Alarm", time = "08:00",
            amPm = "AM", category = "MEDICINE", isActive = true
        )

        val result = alarmHelper.scheduleAlarm(alarm)
        assertFalse(result)
    }

    @Test
    fun `scheduleAlarm returns true when canScheduleExactAlarm is true`() {
        alarmHelper = spyk(AlarmHelper(context))
        every { alarmHelper.canScheduleExactAlarm() } returns true
        every { context.packageName } returns "com.example.smarthealthreminder"

        val intent = mockk<android.content.Intent>(relaxed = true)
        val pendingIntent = mockk<android.app.PendingIntent>(relaxed = true)

        val alarm = Alarm(
            id = "a1", label = "Test Alarm", time = "08:00",
            amPm = "AM", category = "MEDICINE", isActive = true,
            repeatDays = null
        )

        val result = alarmHelper.scheduleAlarm(alarm)
        assertTrue(result)
    }

    // ───── snoozeAlarm ─────

    @Test
    fun `snoozeAlarm returns false when canScheduleExactAlarm is false`() {
        alarmHelper = spyk(AlarmHelper(context))
        every { alarmHelper.canScheduleExactAlarm() } returns false

        val alarm = Alarm(id = "a1", label = "Test", time = "08:00", amPm = "AM", category = "MEDICINE")
        val result = alarmHelper.snoozeAlarm(alarm, 10)
        assertFalse(result)
    }

    @Test
    fun `snoozeAlarm returns true when canScheduleExactAlarm is true`() {
        alarmHelper = spyk(AlarmHelper(context))
        every { alarmHelper.canScheduleExactAlarm() } returns true
        every { context.packageName } returns "com.example.smarthealthreminder"

        val alarm = Alarm(id = "a1", label = "Test", time = "08:00", amPm = "AM", category = "MEDICINE")
        val result = alarmHelper.snoozeAlarm(alarm, 10)
        assertTrue(result)
    }

    @Test
    fun `parseRepeatDays handles mixed formats and case`() {
        val days = AlarmHelper.parseRepeatDays("mon, WED;Fri Sun")
        assertEquals(4, days.size)
        assertTrue(days.contains(Calendar.MONDAY))
        assertTrue(days.contains(Calendar.WEDNESDAY))
        assertTrue(days.contains(Calendar.FRIDAY))
        assertTrue(days.contains(Calendar.SUNDAY))
    }

    @Test
    fun `parseRepeatDays returns empty for malformed input`() {
        assertTrue(AlarmHelper.parseRepeatDays("invalid gibberish").isEmpty())
        assertTrue(AlarmHelper.parseRepeatDays(null).isEmpty())
        assertTrue(AlarmHelper.parseRepeatDays("").isEmpty())
    }

    @Test
    fun `parseRepeatDays deduplicates days`() {
        val days = AlarmHelper.parseRepeatDays("Mon Mon Tue")
        assertEquals(2, days.size)
    }

    @Test
    fun `isValidTime rejects invalid values`() {
        assertFalse(AlarmHelper.isValidTime(25, 0))
        assertFalse(AlarmHelper.isValidTime(8, 60))
        assertTrue(AlarmHelper.isValidTime(8, 30))
    }

    // ───── Alarm model ─────

    @Test
    fun `Alarm data class equality works correctly`() {
        val a1 = Alarm(id = "1", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        val a2 = Alarm(id = "1", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        assertTrue(a1 == a2)
    }

    @Test
    fun `Alarm with different IDs are not equal`() {
        val a1 = Alarm(id = "1", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        val a2 = Alarm(id = "2", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        assertFalse(a1 == a2)
    }

    @Test
    fun `Alarm isActive defaults to true`() {
        val alarm = Alarm(id = "1", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        assertTrue(alarm.isActive)
    }

    @Test
    fun `Alarm copy works correctly`() {
        val original = Alarm(id = "1", label = "Med", time = "08:00", amPm = "AM", category = "MEDICINE")
        val copy = original.copy(isActive = false)
        assertFalse(copy.isActive)
        assertEquals(original.id, copy.id)
    }

    @Test
    fun `Alarm Parcelable CREATOR creates array of correct size`() {
        val array = Alarm.newArray(5)
        assertEquals(5, array.size)
    }
}
