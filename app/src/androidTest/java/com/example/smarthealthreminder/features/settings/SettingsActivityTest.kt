package com.example.smarthealthreminder.features.settings

import androidx.test.core.app.ActivityScenario
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
class SettingsActivityTest {

    @Test
    fun settingsActivity_launchesSuccessfully() {
        ActivityScenario.launch(SettingsActivity::class.java)
        onView(withId(R.id.switch_notifications)).check(matches(isDisplayed()))
    }

    @Test
    fun backButton_finishesActivity() {
        ActivityScenario.launch(SettingsActivity::class.java).use { scenario ->
            onView(withId(R.id.btn_back)).perform(click())
            scenario.onActivity { assert(it.isFinishing || it.isDestroyed) }
        }
    }

    @Test
    fun themeSpinner_isVisible() {
        ActivityScenario.launch(SettingsActivity::class.java)
        onView(withId(R.id.spinner_theme_mode)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigation_isVisible() {
        ActivityScenario.launch(SettingsActivity::class.java)
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }
}
