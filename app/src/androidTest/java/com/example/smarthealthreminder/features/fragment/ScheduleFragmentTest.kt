package com.example.smarthealthreminder.features.fragment

import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
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
class ScheduleFragmentTest {

    @Test
    fun scheduleFragment_launchesSuccessfully() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.recycler_calendar)).check(matches(isDisplayed()))
    }

    @Test
    fun monthNavigation_buttonsAreVisible() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_prev_month)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_next_month)).check(matches(isDisplayed()))
    }

    @Test
    fun noteSection_isVisible() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.et_note)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_save_note)).check(matches(isDisplayed()))
    }

    @Test
    fun clickPrevMonth_updatesCalendar() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_prev_month)).perform(click())
        onView(withId(R.id.recycler_calendar)).check(matches(isDisplayed()))
    }

    @Test
    fun clickNextMonth_updatesCalendar() {
        launchFragmentInContainer<ScheduleFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_next_month)).perform(click())
        onView(withId(R.id.recycler_calendar)).check(matches(isDisplayed()))
    }
}
