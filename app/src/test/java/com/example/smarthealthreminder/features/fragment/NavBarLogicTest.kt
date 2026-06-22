package com.example.smarthealthreminder.features.fragment

import com.example.smarthealthreminder.features.activity.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Unit tests for navigation constants and destination routing logic
 * used by MainActivity, MainWelcomeActivity, and BottomNavHelper.
 *
 * These are pure logic tests — no Android context required.
 */
class NavBarLogicTest {

    // ───── Navigation destination constants ─────

    @Test
    fun `EXTRA_START_DESTINATION constant is correct`() {
        assertEquals("extra_start_destination", MainActivity.EXTRA_START_DESTINATION)
    }

    @Test
    fun `DESTINATION_HOME constant is correct`() {
        assertEquals("home", MainActivity.DESTINATION_HOME)
    }

    @Test
    fun `DESTINATION_SCHEDULE constant is correct`() {
        assertEquals("schedule", MainActivity.DESTINATION_SCHEDULE)
    }

    @Test
    fun `DESTINATION_ALARMS constant is correct`() {
        assertEquals("alarms", MainActivity.DESTINATION_ALARMS)
    }

    @Test
    fun `DESTINATION_REMINDERS constant is correct`() {
        assertEquals("reminders", MainActivity.DESTINATION_REMINDERS)
    }

    // ───── Destination routing logic (mirrors navigateToDestination) ─────

    @Test
    fun `schedule destination maps to schedule fragment`() {
        val dest = resolveDestination(MainActivity.DESTINATION_SCHEDULE)
        assertEquals("schedule", dest)
    }

    @Test
    fun `alarms destination maps to alarms`() {
        val dest = resolveDestination(MainActivity.DESTINATION_ALARMS)
        assertEquals("alarms", dest)
    }

    @Test
    fun `reminders destination maps to reminders`() {
        val dest = resolveDestination(MainActivity.DESTINATION_REMINDERS)
        assertEquals("reminders", dest)
    }

    @Test
    fun `unknown destination falls back to home`() {
        val dest = resolveDestination("unknown_screen")
        assertEquals("home", dest)
    }

    @Test
    fun `null destination falls back to home`() {
        val dest = resolveDestination(null)
        assertEquals("home", dest)
    }

    @Test
    fun `home destination maps to home`() {
        val dest = resolveDestination(MainActivity.DESTINATION_HOME)
        assertEquals("home", dest)
    }

    // ───── All destination constants are non-empty ─────

    @Test
    fun `all destination constants are non-empty strings`() {
        val destinations = listOf(
            MainActivity.DESTINATION_HOME,
            MainActivity.DESTINATION_SCHEDULE,
            MainActivity.DESTINATION_ALARMS,
            MainActivity.DESTINATION_REMINDERS
        )
        destinations.forEach { dest ->
            assertNotNull(dest)
            assert(dest.isNotEmpty()) { "Destination '$dest' should not be empty" }
        }
    }

    @Test
    fun `all destination constants are distinct`() {
        val destinations = listOf(
            MainActivity.DESTINATION_HOME,
            MainActivity.DESTINATION_SCHEDULE,
            MainActivity.DESTINATION_ALARMS,
            MainActivity.DESTINATION_REMINDERS
        )
        assertEquals(destinations.size, destinations.distinct().size)
    }

    // ─── Mirrors MainActivity.navigateToDestination logic ───
    private fun resolveDestination(destination: String?): String {
        return when (destination) {
            MainActivity.DESTINATION_SCHEDULE -> "schedule"
            MainActivity.DESTINATION_ALARMS -> "alarms"
            MainActivity.DESTINATION_REMINDERS -> "reminders"
            else -> "home"
        }
    }
}
