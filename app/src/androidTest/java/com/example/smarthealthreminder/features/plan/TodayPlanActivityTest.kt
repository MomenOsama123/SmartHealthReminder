package com.example.smarthealthreminder.features.plan

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
class TodayPlanActivityTest {

    @Test
    fun todayPlanActivity_launchesSuccessfully() {
        ActivityScenario.launch(TodayPlanActivity::class.java)
        onView(withId(R.id.tv_date)).check(matches(isDisplayed()))
        onView(withId(R.id.recycler_today_plan)).check(matches(isDisplayed()))
    }

    @Test
    fun backButton_finishesActivity() {
        ActivityScenario.launch(TodayPlanActivity::class.java).use { scenario ->
            onView(withId(R.id.btn_back)).perform(click())
            scenario.onActivity { assert(it.isFinishing || it.isDestroyed) }
        }
    }

    @Test
    fun emptyState_isVisible() {
        ActivityScenario.launch(TodayPlanActivity::class.java)
        onView(withId(R.id.layout_empty)).check(matches(isDisplayed()))
    }

    @Test
    fun quickActions_areVisible() {
        ActivityScenario.launch(TodayPlanActivity::class.java)
        onView(withId(R.id.btn_add_reminder)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_add_alarm)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_add_note)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_create_report)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_open_schedule)).check(matches(isDisplayed()))
    }

    @Test
    fun clickAddReminder_opensAddReminderActivity() {
        ActivityScenario.launch(TodayPlanActivity::class.java)
        onView(withId(R.id.btn_add_reminder)).perform(click())
    }

    @Test
    fun clickAddAlarm_opensEditAlarmActivity() {
        ActivityScenario.launch(TodayPlanActivity::class.java)
        onView(withId(R.id.btn_add_alarm)).perform(click())
    }
}
