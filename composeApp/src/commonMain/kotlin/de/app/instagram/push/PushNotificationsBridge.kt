package de.app.instagram.push

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class PushNotificationEvent(
    val title: String?,
    val body: String?,
    val data: Map<String, String> = emptyMap(),
)

object PushNotificationsBridge {
    private val _deviceToken = MutableStateFlow<String?>(null)
    val deviceToken: StateFlow<String?> = _deviceToken.asStateFlow()

    private val _events = MutableSharedFlow<PushNotificationEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<PushNotificationEvent> = _events.asSharedFlow()

    fun onTokenReceived(token: String) {
        _deviceToken.value = token
    }

    fun onNotificationReceived(
        title: String?,
        body: String?,
        data: Map<String, String> = emptyMap(),
    ) {
        _events.tryEmit(PushNotificationEvent(title = title, body = body, data = data))
    }
}
