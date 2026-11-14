package com.neoland.app.island

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
 * Nayland 2.0: Aktivitas Live berbentuk kapsul untuk Kalkulator, Kecepatan Jaringan, dan Info Perangkat.
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

    fun anyActive(): Boolean = calcOn.value || netOn.value || deviceOn.value

    fun open(a: QuickAction, context: Context) {
        val appCtx = context.applicationContext
        when (a) {
            QuickAction.CALCULATOR -> calcOn.value = true
            QuickAction.NETWORK -> {
                netOn.value = true
                NetLive.start(appCtx)
            }
            QuickAction.DEVICE -> {
                deviceOn.value = true
                DeviceLive.start(appCtx)
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

    fun closeAll() {
        calcOn.value = false
        netOn.value = false
        deviceOn.value = false
        NetLive.stop()
        DeviceLive.stop()
    }
}

object CalcLive {
    val engine = CalcEngine()
    val tick = mutableIntStateOf(0)
}

object DeviceLive {
    val snap = mutableStateOf<DeviceSnapshot?>(null)
    private var job: Job? = null

    fun start(context: Context) {
        val appCtx = context.applicationContext
        if (job?.isActive == true) return
        runCatching { snap.value = DeviceStats.read(appCtx) }
        job = IslandBus.scope.launch {
            var wasOff = false
            while (true) {
                delay(2000)
                if (IslandBus.screenOn.value) {
                    if (wasOff) wasOff = false
                    runCatching { snap.value = DeviceStats.read(appCtx) }
                } else {
                    wasOff = true
                }
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
    val fontSize = when {
        text.length > 10 -> 12.sp
        text.length > 7 -> 14.sp
        else -> 16.sp
    }
    CompactRow(
        h, compactGap(s), sideMax(s) * 1.65f,
        left = { LiveCardIcon(QuickAction.CALCULATOR, h * 0.5f, Tints.Orange) },
        right = {
            Text(
                text, color = Color.White, fontSize = fontSize, style = Digits,
                maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis,
            )
        },
    )
}

@Composable
fun NetCompact(s: IslandSettings) {
    val h = activeHeight(s)
    val ok = NetLive.supported.value
    val down = if (ok) "↓ " + NetSpeed.speedText(NetLive.down.value, Lang.locale) else "–"
    val up = if (ok) "↑ " + NetSpeed.speedText(NetLive.up.value, Lang.locale) else ""
    CompactRow(
        h, compactGap(s), sideMax(s) * 1.85f,
        left = { Text(down, color = Tints.Green, fontSize = 12.sp, style = Digits, maxLines = 1, softWrap = false) },
        right = { Text(up, color = Tints.Blue, fontSize = 12.sp, style = Digits, maxLines = 1, softWrap = false) },
    )
}

@Composable
fun DeviceCompact(s: IslandSettings) {
    val h = activeHeight(s)
    val d = DeviceLive.snap.value
    val pct = if (d != null && d.ramTotal > 0L) (d.ramUsed * 100L / d.ramTotal).toInt() else 0
    val temp = d?.batteryTempC
    val tempColor = when {
        temp == null -> Tints.Orange
        temp >= 42f -> Color(0xFFFF453A)
        temp >= 38f -> Tints.Orange
        else -> Tints.Green
    }
    val tempText = if (temp == null) "-" else String.format(Lang.locale, "%.0f°", temp)
    CompactRow(
        h, compactGap(s), sideMax(s) * 1.65f,
        left = { Text(tempText, color = tempColor, fontSize = 14.sp, style = Digits, maxLines = 1, softWrap = false) },
        right = { Text("RAM $pct%", color = Tints.Blue, fontSize = 14.sp, style = Digits, maxLines = 1, softWrap = false) },
    )
}

@Composable
fun LiveCardExpanded(a: QuickAction) {
    val dotColor = when (a) {
        QuickAction.CALCULATOR -> Tints.Orange
        QuickAction.NETWORK -> Tints.Green
        else -> Tints.Blue
    }
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(dotColor))
                Spacer(Modifier.width(7.dp))
                Text(
                    a.short,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            RoundButton(Ios.close, size = 30.dp, bg = Color.White.copy(alpha = 0.15f), iconScale = 0.5f) {
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
