package com.example.smarthealthreminder.features.profileInfo.ProfileInfo

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
class ReportsActivityTest {

    @Test
    fun reportsActivity_launchesSuccessfully() {
        ActivityScenario.launch(ReportsActivity::class.java)
        onView(withId(R.id.reportsActivity)).check(matches(isDisplayed()))
    }

    @Test
    fun backButton_finishesActivity() {
        ActivityScenario.launch(ReportsActivity::class.java).use { scenario ->
            onView(withId(R.id.btn_back)).perform(click())
            scenario.onActivity { assert(it.isFinishing || it.isDestroyed) }
        }
    }

    @Test
    fun downloadButton_isVisible() {
        ActivityScenario.launch(ReportsActivity::class.java)
        onView(withId(R.id.download_btn)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavigation_isVisible() {
        ActivityScenario.launch(ReportsActivity::class.java)
        onView(withId(R.id.bottom_navigation)).check(matches(isDisplayed()))
    }

    @Test
    fun savedReportsSection_isVisible() {
        ActivityScenario.launch(ReportsActivity::class.java)
        onView(withId(R.id.recycler_reports)).check(matches(isDisplayed()))
    }
}
