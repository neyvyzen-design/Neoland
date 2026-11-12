package com.neoland.app.core
import kotlinx.coroutines.flow.MutableStateFlow

/** State khusus untuk hardware dan status OS. */
object NeolandDeviceState {
    val screenOn = MutableStateFlow(true)
    val powerSave = MutableStateFlow(false)
    val battery = MutableStateFlow(BatteryInfo())
    val locked = MutableStateFlow(false)
    val keyguard = MutableStateFlow(false)
    val glassTilt = MutableStateFlow(0f)
    val landscape = MutableStateFlow(false)
}
