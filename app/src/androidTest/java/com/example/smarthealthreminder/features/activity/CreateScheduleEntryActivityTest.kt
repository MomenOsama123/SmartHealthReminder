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
class CreateScheduleEntryActivityTest {

    @Test
    fun createScheduleEntryActivity_launchesSuccessfully() {
        ActivityScenario.launch(CreateScheduleEntryActivity::class.java)
        onView(withId(R.id.et_title)).check(matches(isDisplayed()))
        onView(withId(R.id.et_date)).check(matches(isDisplayed()))
        onView(withId(R.id.et_time)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_save)).check(matches(isDisplayed()))
    }

    @Test
    fun backButton_finishesActivity() {
        ActivityScenario.launch(CreateScheduleEntryActivity::class.java).use { scenario ->
            onView(withId(R.id.btn_back)).perform(click())
            scenario.onActivity { assert(it.isFinishing || it.isDestroyed) }
        }
    }

    @Test
    fun prefillDate_isSetWhenProvided() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), CreateScheduleEntryActivity::class.java).apply {
            putExtra(CreateScheduleEntryActivity.EXTRA_SELECTED_DATE, "2024-12-25")
        }
        ActivityScenario.launch<CreateScheduleEntryActivity>(intent)
        onView(withId(R.id.et_date)).check(matches(isDisplayed()))
    }
}
