package com.neoland.app.island

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.Controls
import com.neoland.app.core.IslandBus
import com.neoland.app.core.Lang
import com.neoland.app.core.Tints
import com.neoland.app.data.IslandSettings
import com.neoland.app.data.QuickAction
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 1.7.17: Kalkulator, Kecepatan, dan Perangkat sebagai aktivitas live. Aktivitas hidup selama belum ditutup
 * (tombol X di kartu besar). Pembacaan data (kecepatan, perangkat) hanya jalan selama aktivitasnya ada
 * dan layar menyala.
 */
object LiveCards {
    val calcOn = mutableStateOf(false)
    val netOn = mutableStateOf(false)
    val deviceOn = mutableStateOf(false)

    fun idOf(a: QuickAction): String? = when (a) {
        QuickAction.CALCULATOR -> "calc"
        QuickAction.NETWORK -> "net"
        QuickAction.DEVICE -> "device"
        else -> null
    }

    fun open(a: QuickAction, appContext: Context) {
        when (a) {
            QuickAction.CALCULATOR -> calcOn.value = true
            QuickAction.NETWORK -> {
                netOn.value = true
                NetLive.start(appContext)
            }
            QuickAction.DEVICE -> {
                deviceOn.value = true
                DeviceLive.start(appContext)
            }
            else -> Unit
        }
    }

    fun close(a: QuickAction) {
        when (a) {
            QuickAction.CALCULATOR -> calcOn.value = false
            QuickAction.NETWORK -> {
                netOn.value = false
                NetLive.stop()
            }
            QuickAction.DEVICE -> {
                deviceOn.value = false
                DeviceLive.stop()
            }
            else -> Unit
        }
    }
}

/** Mesin kalkulator bersama: kapsul kecil dan kartu besar menampilkan hasil yang sama. */
object CalcLive {
    val engine = CalcEngine()
    val tick = mutableIntStateOf(0)
}

object DeviceLive {
    val snap = mutableStateOf<DeviceSnapshot?>(null)
    private var job: Job? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        snap.value = DeviceStats.read(context)
        job = IslandBus.scope.launch {
            while (true) {
                delay(2000)
                if (IslandBus.screenOn.value) snap.value = DeviceStats.read(context)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

@Composable
fun LiveCardIcon(a: QuickAction, size: Dp, tint: Color) {
    val icon = when (a) {
        QuickAction.CALCULATOR -> Icons.Rounded.Calculate
        QuickAction.NETWORK -> Icons.Rounded.Speed
        else -> Icons.Rounded.PhoneAndroid
    }
    Icon(icon, null, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun CalcCompact(s: IslandSettings) {
    val h = activeHeight(s)
    @Suppress("UNUSED_VARIABLE")
    val version = CalcLive.tick.intValue
    val text = CalcLive.engine.display
    CompactRow(h, compactGap(s), sideMax(s) * 1.6f,
        left = { LiveCardIcon(QuickAction.CALCULATOR, h * 0.5f, Tints.Orange) },
        right = {
            Text(text, color = Color.White, fontSize = 16.sp, style = Digits, maxLines = 1, softWrap = false, overflow = TextOverflow.Clip)
        },
    )
}

@Composable
fun NetCompact(s: IslandSettings) {
    val h = activeHeight(s)
    val ok = NetLive.supported.value
    val down = if (ok) "↓ " + NetSpeed.speedText(NetLive.down.value, Lang.locale) else "–"
    val up = if (ok) "↑ " + NetSpeed.speedText(NetLive.up.value, Lang.locale) else ""
    CompactRow(h, compactGap(s), sideMax(s) * 1.8f,
        left = { Text(down, color = Tints.Green, fontSize = 13.sp, style = Digits, maxLines = 1, softWrap = false) },
        right = { Text(up, color = Tints.Blue, fontSize = 13.sp, style = Digits, maxLines = 1, softWrap = false) },
    )
}

@Composable
fun DeviceCompact(s: IslandSettings) {
    val h = activeHeight(s)
    val d = DeviceLive.snap.value
    val pct = if (d != null && d.ramTotal > 0L) (d.ramUsed * 100L / d.ramTotal).toInt() else 0
    val temp = d?.batteryTempC
    val tempText = if (temp == null) "-" else String.format(Lang.locale, "%.0f°", temp)
    CompactRow(h, compactGap(s), sideMax(s) * 1.6f,
        left = { Text(tempText, color = Tints.Orange, fontSize = 15.sp, style = Digits, maxLines = 1, softWrap = false) },
        right = { Text("RAM $pct%", color = Tints.Blue, fontSize = 15.sp, style = Digits, maxLines = 1, softWrap = false) },
    )
}

/** Kartu besar: judul, tombol X (menutup aktivitasnya), lalu isi kartu. */
@Composable
fun LiveCardExpanded(a: QuickAction) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(a.short, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp, maxLines = 1)
            RoundButton(Ios.close, size = 32.dp, bg = Color.White.copy(alpha = 0.15f), iconScale = 0.5f) {
                LiveCards.close(a)
                Controls.collapse()
            }
        }
        Spacer(Modifier.height(10.dp))
        when (a) {
            QuickAction.CALCULATOR -> CalcCard()
            QuickAction.NETWORK -> NetSpeedCard()
            QuickAction.DEVICE -> DeviceCard()
            else -> Unit
        }
    }
}
