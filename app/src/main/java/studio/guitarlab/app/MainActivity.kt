package studio.guitarlab.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import studio.guitarlab.app.activity.AppNotificationDeepLink
import studio.guitarlab.app.ui.GuitarLabApp
import studio.guitarlab.app.ui.ExternalControlHub
import studio.guitarlab.app.ui.theme.GuitarLabTheme

class MainActivity : ComponentActivity() {
    private val notificationRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        notificationRoute.value = AppNotificationDeepLink.route(intent)
        ExternalControlHub.initialize(this)
        enterImmersiveMode()
        setContent {
            GuitarLabTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().semantics { testTagsAsResourceId = true },
                    color = MaterialTheme.colorScheme.background,
                ) {
                    GuitarLabApp(
                        deepLinkRoute = notificationRoute.value,
                        onDeepLinkConsumed = { notificationRoute.value = null },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        notificationRoute.value = AppNotificationDeepLink.route(intent)
    }

    override fun onResume() {
        super.onResume()
        ExternalControlHub.setForeground(true)
    }

    override fun onPause() {
        ExternalControlHub.setForeground(false)
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (handleExternalHid(event)) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (handleExternalHid(event)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun handleExternalHid(event: KeyEvent): Boolean {
        val token = ExternalControlHub.hidToken(event) ?: return false
        if (!ExternalControlHub.shouldCaptureHid(this, token)) return false
        ExternalControlHub.onHidEvent(this, token, event)
        return true
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) enterImmersiveMode()
    }

    private fun enterImmersiveMode() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}
