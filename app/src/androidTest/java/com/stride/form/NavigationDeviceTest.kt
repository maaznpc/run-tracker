package com.stride.form

import android.graphics.Bitmap
import android.content.ContentValues
import android.os.Build
import android.provider.MediaStore
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
        try {
            val context = instrumentation.targetContext
            if (Build.VERSION.SDK_INT >= 29) {
                // Public test images survive AGP uninstalling the app after instrumentation.
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "$name.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Stride-Verification")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val image = requireNotNull(resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values))
                requireNotNull(resolver.openOutputStream(image)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                resolver.update(image, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            } else {
                val directory = File(context.getExternalFilesDir(null), "verification").apply { mkdirs() }
                File(directory, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
        } finally { bitmap.recycle() }
    }
}
