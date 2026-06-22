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
class HomeFragmentTest {

    @Test
    fun homeFragment_launchesSuccessfully() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.content_container)).check(matches(isDisplayed()))
    }

    @Test
    fun addReminderButton_isVisible() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_add_reminder)).check(matches(isDisplayed()))
    }

    @Test
    fun actionButtons_areVisible() {
        launchFragmentInContainer<HomeFragment>(themeResId = R.style.Theme_SmartHealthReminder)
        onView(withId(R.id.btn_mark_done)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_edit)).check(matches(isDisplayed()))
        onView(withId(R.id.btn_delete)).check(matches(isDisplayed()))
    }
}
