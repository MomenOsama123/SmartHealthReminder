package com.example.smarthealthreminder.features.schedule.details

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
class DayDetailsActivityTest {

    @Test
    fun dayDetailsActivity_launchesSuccessfully() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DayDetailsActivity::class.java).apply {
            putExtra(DayDetailsActivity.EXTRA_DATE, "2024-06-15")
            putExtra(DayDetailsActivity.EXTRA_DATE_DISPLAY, "Saturday, Jun 15")
        }
        ActivityScenario.launch<DayDetailsActivity>(intent)
        onView(withId(R.id.tv_date)).check(matches(isDisplayed()))
        onView(withId(R.id.recycler_events)).check(matches(isDisplayed()))
    }

    @Test
    fun backButton_finishesActivity() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DayDetailsActivity::class.java).apply {
            putExtra(DayDetailsActivity.EXTRA_DATE, "2024-06-15")
        }
        ActivityScenario.launch<DayDetailsActivity>(intent).use { scenario ->
            onView(withId(R.id.btn_back)).perform(click())
            scenario.onActivity { assert(it.isFinishing || it.isDestroyed) }
        }
    }

    @Test
    fun noteSection_isVisible() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DayDetailsActivity::class.java).apply {
            putExtra(DayDetailsActivity.EXTRA_DATE, "2024-06-15")
        }
        ActivityScenario.launch<DayDetailsActivity>(intent)
        onView(withId(R.id.tv_empty_message)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_add_note)).check(matches(isDisplayed()))
    }

    @Test
    fun quickActions_areVisible() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DayDetailsActivity::class.java).apply {
            putExtra(DayDetailsActivity.EXTRA_DATE, "2024-06-15")
        }
        ActivityScenario.launch<DayDetailsActivity>(intent)
        onView(withId(R.id.btn_add_reminder)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_add_alarm)).check(matches(isDisplayed()))
    }
}
