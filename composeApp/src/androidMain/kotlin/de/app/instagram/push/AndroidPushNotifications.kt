package de.app.instagram.push

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging

object AndroidPushNotifications {
    private const val TAG: String = "AndroidPushNotifications"
    const val CHANNEL_ID: String = "instagram_clone_push"
    private const val CHANNEL_NAME: String = "Instagram Notifications"
    private const val POST_NOTIFICATIONS_REQUEST_CODE: Int = 7001

    fun initialize(context: Context, activity: Activity?) {
        val app = FirebaseApp.initializeApp(context)
        createNotificationChannel(context)
        requestNotificationPermissionIfNeeded(activity)

        if (app == null) {
            Log.w(
                TAG,
                "FirebaseApp is not initialized. Add google-services.json or manual Firebase options.",
            )
            return
        }

        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                PushNotificationsBridge.onTokenReceived(token)
                Log.d(TAG, "FCM token received")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to fetch FCM token", error)
            }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    private fun requestNotificationPermissionIfNeeded(activity: Activity?) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val host = activity ?: return

        val granted = ContextCompat.checkSelfPermission(
            host,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            ActivityCompat.requestPermissions(
                host,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                POST_NOTIFICATIONS_REQUEST_CODE,
            )
        }
    }
}
