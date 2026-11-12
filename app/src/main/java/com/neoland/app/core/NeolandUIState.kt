package com.neoland.app.core
import kotlinx.coroutines.flow.MutableStateFlow

/** State khusus untuk interaksi UI Jetpack Compose. */
object NeolandUIState {
    val expanded = MutableStateFlow(false)
    val focus = MutableStateFlow<String?>(null)
    val calibrating = MutableStateFlow(false)
    val calibrationLockProfile = MutableStateFlow(false)
    val widthPreviewUntil = MutableStateFlow(0L)
    val reply = MutableStateFlow<ReplyState?>(null)
}
