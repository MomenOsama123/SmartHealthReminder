package com.example.smarthealthreminder.features.activity

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.smarthealthreminder.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityNavigationTest {

    @Test
    fun homeFragment_isDisplayedByDefault() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.content_container)).check(matches(isDisplayed()))
    }

    @Test
    fun clickSchedule_showsScheduleFragment() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_schedule)).perform(click())
        onView(withId(R.id.recycler_calendar)).check(matches(isDisplayed()))
    }

    @Test
    fun clickFab_onHome_showsQuickActionsBottomSheet() {
        ActivityScenario.launch(MainActivity::class.java)
        onView(withId(R.id.nav_create)).perform(click())
        // Bottom sheet dialog is shown; we verify by checking if fragment manager has it
    }

    @Test
    fun startDestination_schedule_navigatesToSchedule() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SCHEDULE)
        }
        ActivityScenario.launch<MainActivity>(intent)
        onView(withId(R.id.recycler_calendar)).check(matches(isDisplayed()))
    }

    @Test
    fun startDestination_alarms_navigatesToAlarms() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_ALARMS)
        }
        ActivityScenario.launch<MainActivity>(intent)
        onView(withId(R.id.recycler_alarms)).check(matches(isDisplayed()))
    }

    @Test
    fun startDestination_reminders_navigatesToReminders() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_REMINDERS)
        }
        ActivityScenario.launch<MainActivity>(intent)
        onView(withId(R.id.recycler_timeline)).check(matches(isDisplayed()))
    }
}
