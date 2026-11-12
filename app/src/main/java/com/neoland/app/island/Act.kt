package com.neoland.app.island

import com.neoland.app.core.AlarmInfo
import com.neoland.app.core.BatteryInfo
import com.neoland.app.core.CallInfo
import com.neoland.app.core.ChronoInfo
import com.neoland.app.core.LiveInfo
import com.neoland.app.core.MediaInfo
import com.neoland.app.core.Notice
import com.neoland.app.core.PayReady
import com.neoland.app.core.ProgressInfo
import com.neoland.app.core.StopwatchState
import com.neoland.app.core.TimerState
import com.neoland.app.core.TorchState

/** То, что сейчас может показывать остров. Короткие (transient) не раскрываются и не уходят в пузырь. */
sealed class Act(val id: String, val transient: Boolean = false) {
    open val pkg: String? get() = null

    data object Idle : Act("idle")
    data class NoticeA(val n: Notice) : Act("notice", transient = true)
    data class TimerA(val t: TimerState) : Act("timer")
    data class StopwatchA(val w: StopwatchState) : Act("stopwatch")
    data class TorchA(val t: TorchState) : Act("torch")
    data class ChronoA(val c: ChronoInfo) : Act("chrono") {
        override val pkg: String get() = c.pkg
    }
    data class MediaA(val m: MediaInfo) : Act("media") {
        override val pkg: String get() = m.pkg
    }
    data class ProgressA(val p: ProgressInfo) : Act("progress") {
        override val pkg: String get() = p.pkg
    }
    data class CallA(val c: CallInfo) : Act("call") {
        override val pkg: String get() = c.pkg
    }
    data class LiveA(val l: LiveInfo) : Act("live") {
        override val pkg: String get() = l.pkg
    }
    data class ChargingA(val b: BatteryInfo) : Act("charging")
    /** Оплата телефоном: ждём терминал. Не раскрывается и не уходит в пузырь */
    data class PayA(val p: PayReady) : Act("pay", transient = true) {
        override val pkg: String get() = p.pkg
    }
    data class AlarmA(val a: AlarmInfo) : Act("alarm") {
        override val pkg: String? get() = a.pkg
    }
    /** 1.7.17: Kalkulator, Kecepatan, Perangkat — aktivitas live dari halaman opsi */
    data object CalcA : Act("calc")
    data object NetA : Act("net")
    data object DeviceA : Act("device")
}
