package studio.guitarlab.app

import android.annotation.TargetApi
import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

private const val COHESION_SCREENSHOT_RELATIVE_PATH = "Pictures/guitarlab-ci-screenshots"

/**
 * Deterministic screenshot artifact helper for the U10/C8 cohesion gate.
 *
 * UiAutomation owns the full-display capture while MediaStore owns publication.
 * This avoids compound shell-command parsing and scoped-storage permissions,
 * and leaves a shared artifact that survives test-package teardown long enough
 * for scripts/ci_run_api36_regression_groups.sh to collect it with adb.
 */
@TargetApi(Build.VERSION_CODES.Q)
fun ComposeTestRule.captureCohesionScreenshot(name: String) {
    waitForIdle()

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val safeName = name
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "screenshot" }
    val displayName = "$safeName.png"
    val resolver = instrumentation.targetContext.contentResolver
    val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    val relativePath = "$COHESION_SCREENSHOT_RELATIVE_PATH/"

    resolver.delete(
        collection,
        "${MediaStore.Images.Media.DISPLAY_NAME} = ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
        arrayOf(displayName, relativePath),
    )

    val screenshot = checkNotNull(instrumentation.uiAutomation.takeScreenshot()) {
        "UiAutomation returned no screenshot for $displayName"
    }
    val values = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }
    val uri = checkNotNull(resolver.insert(collection, values)) {
        "MediaStore refused screenshot publication for $displayName"
    }

    try {
        val compressed = resolver.openOutputStream(uri, "w").use { stream ->
            checkNotNull(stream) { "MediaStore returned no stream for $displayName" }
            screenshot.compress(Bitmap.CompressFormat.PNG, 100, stream)
        }
        check(compressed) { "PNG compression failed for $displayName" }
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        check(resolver.update(uri, values, null, null) == 1) {
            "MediaStore did not publish $displayName"
        }
    } catch (failure: Throwable) {
        resolver.delete(uri, null, null)
        throw failure
    } finally {
        screenshot.recycle()
    }
}
