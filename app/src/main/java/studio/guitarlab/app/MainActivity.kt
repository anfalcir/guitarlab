package studio.guitarlab.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import studio.guitarlab.app.ui.GuitarLabApp
import studio.guitarlab.app.ui.theme.GuitarLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GuitarLabTheme {
                GuitarLabApp()
            }
        }
    }
}
