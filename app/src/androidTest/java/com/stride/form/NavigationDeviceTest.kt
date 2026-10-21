package com.stride.form

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class NavigationDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun homeAndIllustratedReportOpenWithoutCrashing() {
        compose.onNodeWithText("Every stride.", substring = true).assertIsDisplayed()
        screenshot("home")
        compose.onNodeWithText("Take a look around").performScrollTo().performClick()
        compose.onNodeWithText("Example report").assertIsDisplayed()
        compose.onNodeWithText("FORM CONSISTENCY").assertIsDisplayed()
        screenshot("report")
    }

    @Test fun setupRearViewHistoryAndLearnAreReachable() {
        compose.onNodeWithText("Analyze", substring = false).performClick()
        compose.onNodeWithText("Rear view").performClick()
        compose.onNodeWithText("Rear view measures", substring = true).performScrollTo().assertIsDisplayed()
        screenshot("capture-setup")
        compose.onNodeWithContentDescription("Go back").performClick()
        compose.onNodeWithText("My runs").performClick()
        compose.onNodeWithText("Small steps.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Learn", substring = false).performClick()
        compose.onNodeWithText("Run curious.", substring = true).assertIsDisplayed()
        screenshot("learn")
    }

    private fun screenshot(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = requireNotNull(instrumentation.uiAutomation.takeScreenshot())
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "verification").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
    }
}
