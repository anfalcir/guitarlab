package studio.guitarlab.app

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TargetTabletGeometryInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun windowMatchesTargetTabletLandscapeGeometry() {
        if (InstrumentationRegistry.getArguments().getString("targetGeometry") != "true") return
        composeRule.activityRule.scenario.onActivity { activity ->
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.waitUntil(timeoutMillis = GEOMETRY_TIMEOUT_MS) {
            var matches = false
            composeRule.activityRule.scenario.onActivity { activity ->
                val bounds = activity.windowManager.currentWindowMetrics.bounds
                matches = bounds.width() == TARGET_WIDTH_PX &&
                    bounds.height() == TARGET_HEIGHT_PX &&
                    activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
            }
            matches
        }

        composeRule.activityRule.scenario.onActivity { activity ->
            val bounds = activity.windowManager.currentWindowMetrics.bounds
            assertEquals(TARGET_WIDTH_PX, bounds.width())
            assertEquals(TARGET_HEIGHT_PX, bounds.height())
            assertTrue(bounds.width() > bounds.height())
        }
    }

    private companion object {
        const val TARGET_WIDTH_PX = 1920
        const val TARGET_HEIGHT_PX = 1200
        const val GEOMETRY_TIMEOUT_MS = 20_000L
    }
}
