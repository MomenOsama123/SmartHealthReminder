package com.example.smarthealthreminder.features.settings

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.smarthealthreminder.R
import com.example.smarthealthreminder.features.activity.MainActivity
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsActivityTest {

    private fun launchSettings() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_START_DESTINATION, MainActivity.DESTINATION_SETTINGS)
        }
        ActivityScenario.launch<MainActivity>(intent)
    }

    @Test
    fun settingsScreen_isDisplayed() {
        launchSettings()
        onView(withId(R.id.switch_notifications)).check(matches(isDisplayed()))
    }

    @Test
    fun themeSpinner_isVisible() {
        launchSettings()
        onView(withId(R.id.spinner_theme_mode)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigation_isVisible() {
        launchSettings()
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }
}
