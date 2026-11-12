package com.neoland.app.core
import kotlinx.coroutines.flow.MutableStateFlow

/** State khusus untuk media player, musik, dan lirik. */
object NeolandMediaState {
    val media = MutableStateFlow<MediaInfo?>(null)
    val lyrics = MutableStateFlow<Lyrics?>(null)
    val lyricsStatus = MutableStateFlow<Pair<String, LyricsStatus>?>(null)
    val egg = MutableStateFlow(false)
}
