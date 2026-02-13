package de.app.instagram

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import de.app.instagram.network.AndroidCachePaths
import de.app.instagram.push.AndroidPushNotifications

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AndroidCachePaths.appCacheDirPath = applicationContext.cacheDir.absolutePath
        AndroidPushNotifications.initialize(
            context = applicationContext,
            activity = this,
        )

        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
