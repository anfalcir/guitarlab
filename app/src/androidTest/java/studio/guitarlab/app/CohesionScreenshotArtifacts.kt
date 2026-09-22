package studio.guitarlab.app

import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.platform.app.InstrumentationRegistry

private const val COHESION_SCREENSHOT_ROOT = "/sdcard/guitarlab-ci-screenshots"

/**
 * Deterministic screenshot artifact helper for the U10/C8 cohesion gate.
 *
 * The screenshot is produced by the instrumentation shell directly into
 * shared emulator storage. This survives the Android Gradle test runner
 * teardown long enough for scripts/ci_run_api36_regression_groups.sh to
 * collect the PNG after each connectedDebugAndroidTest invocation.
 */
fun ComposeTestRule.captureCohesionScreenshot(name: String) {
    waitForIdle()

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val safeName = name
        .lowercase()
        .replace(Regex("[^a-z0-9._-]+"), "-")
        .trim('-')
        .ifBlank { "screenshot" }
    val remotePath = "$COHESION_SCREENSHOT_ROOT/$safeName.png"
    val command = """
        mkdir -p $COHESION_SCREENSHOT_ROOT
        rm -f $remotePath
        screencap -p $remotePath
        if [ -s $remotePath ]; then
          echo SCREENSHOT_OK
        else
          echo SCREENSHOT_FAILED
          exit 1
        fi
    """.trimIndent().replace("\n", "; ")

    val descriptor = instrumentation.uiAutomation.executeShellCommand(command)
    val output = ParcelFileDescriptor.AutoCloseInputStream(descriptor).bufferedReader().use { it.readText() }
    check("SCREENSHOT_OK" in output) {
        "Cohesion screenshot was not written: $remotePath · shell=$output"
    }
}
