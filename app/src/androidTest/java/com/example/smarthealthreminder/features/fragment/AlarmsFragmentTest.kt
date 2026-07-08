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
class AlarmsFragmentTest {

    @Test
    fun alarmsFragment_launchesSuccessfully() {
        launchFragmentInContainer<AlarmsFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.recycler_alarms)).check(matches(isDisplayed()))
    }

    @Test
    fun addButton_isVisible() {
        launchFragmentInContainer<AlarmsFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.fab_add_alarm)).check(matches(isDisplayed()))
    }

    @Test
    fun emptyView_isVisible() {
        launchFragmentInContainer<AlarmsFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.tv_empty)).check(matches(isDisplayed()))
    }
}
