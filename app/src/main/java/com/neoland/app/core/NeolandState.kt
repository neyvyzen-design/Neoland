package com.neoland.app.core

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * NEOLAND REVOLUTION: Modularisasi State untuk optimisasi RAM & Re-komposisi.
 * Ini akan secara bertahap menggantikan IslandBus.kt yang berat.
 */
object NeolandState {
    val isScreenOn = MutableStateFlow(true)
    val isPowerSave = MutableStateFlow(false)
    val isLandscape = MutableStateFlow(false)
}
