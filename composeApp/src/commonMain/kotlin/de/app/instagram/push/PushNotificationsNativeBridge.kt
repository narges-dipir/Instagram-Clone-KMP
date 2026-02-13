package de.app.instagram.push

fun onNativePushTokenReceived(token: String) {
    PushNotificationsBridge.onTokenReceived(token)
}

fun onNativePushNotificationReceived(title: String?, body: String?) {
    PushNotificationsBridge.onNotificationReceived(
        title = title,
        body = body,
    )
}
