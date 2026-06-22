package com.example.smarthealthreminder.features.fragment

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.smarthealthreminder.R
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class RemindersFragmentTest {

    @Test
    fun remindersFragment_launchesSuccessfully() {
        launchFragmentInContainer<RemindersFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.recycler_timeline)).check(matches(isDisplayed()))
    }

    @Test
    fun summaryViews_areVisible() {
        launchFragmentInContainer<RemindersFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.tv_today_count)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_missed_count)).check(matches(isDisplayed()))
        onView(withId(R.id.tv_completed_count)).check(matches(isDisplayed()))
    }

    @Test
    fun addReminderButton_isVisible() {
        launchFragmentInContainer<RemindersFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_add_reminder)).check(matches(isDisplayed()))
    }
}
