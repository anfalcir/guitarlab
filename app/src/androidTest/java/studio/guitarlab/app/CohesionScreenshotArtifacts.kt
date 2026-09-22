package studio.guitarlab.app

import android.graphics.Bitmap
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.FileOutputStream

/**
 * Deterministic screenshot artifact helper for the U10/C8 cohesion gate.
 *
 * Screenshots are stored in the target app's internal files directory and
 * collected by scripts/ci_run_api36_regression_groups.sh via adb run-as.
 */
fun ComposeTestRule.captureCohesionScreenshot(name: String) {
    waitForIdle()

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val safeName = name
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "screenshot" }
    val dir = File(instrumentation.targetContext.filesDir, "ci-screenshots").apply { mkdirs() }
    val file = File(dir, "$safeName.png")

    val bitmap = instrumentation.uiAutomation.takeScreenshot()
    FileOutputStream(file).use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "Could not encode cohesion screenshot: $safeName"
        }
    }
    bitmap.recycle()
    check(file.isFile && file.length() > 0L) {
        "Cohesion screenshot was not written: ${file.absolutePath}"
    }
}
