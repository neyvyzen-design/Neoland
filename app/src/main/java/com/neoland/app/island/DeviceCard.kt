package com.neoland.app.island

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.Lang
import com.neoland.app.core.Tints
import com.neoland.app.core.tr

data class DeviceSnapshot(
    val ramUsed: Long,
    val ramTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val batteryTempC: Float?,
    val thermal: String?,
    val batteryPct: Int = -1,
    val isCharging: Boolean = false,
)

object DeviceStats {
    fun read(context: Context): DeviceSnapshot {
        val am = runCatching { context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager }.getOrNull()
        val mi = ActivityManager.MemoryInfo()
        runCatching { am?.getMemoryInfo(mi) }
        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val diskTotal = stat?.totalBytes ?: 0L
        val diskFree = stat?.availableBytes ?: 0L
        val intent: Intent? = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val temp = if (rawTemp == Int.MIN_VALUE) null else rawTemp / 10f
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return DeviceSnapshot(
            ramUsed = (mi.totalMem - mi.availMem).coerceAtLeast(0L),
            ramTotal = mi.totalMem,
            diskUsed = (diskTotal - diskFree).coerceAtLeast(0L),
            diskTotal = diskTotal,
            batteryTempC = temp,
            thermal = thermalLabel(context),
            batteryPct = pct,
            isCharging = charging,
        )
    }

    private fun thermalLabel(context: Context): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val pm = runCatching { context.getSystemService(Context.POWER_SERVICE) as? PowerManager }.getOrNull() ?: return null
        return runCatching {
            when (pm.currentThermalStatus) {
                PowerManager.THERMAL_STATUS_NONE -> tr("Норма", "Normal", "Normal")
                PowerManager.THERMAL_STATUS_LIGHT -> tr("Тепло", "Light", "Hangat")
                PowerManager.THERMAL_STATUS_MODERATE -> tr("Горячо", "Moderate", "Panas")
                PowerManager.THERMAL_STATUS_SEVERE -> tr("Очень горячо", "Severe", "Sangat panas")
                else -> tr("Критично", "Critical", "Kritis")
            }
        }.getOrNull()
    }
}

/** Nayland 2.0: Monitor perangkat real-time (RAM, Penyimpanan, Baterai %, Suhu & Status Termal). */
@Composable
fun DeviceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val d = DeviceLive.snap.value ?: DeviceStats.read(context)
    val temp = d.batteryTempC
    val tempColor = when {
        temp == null -> Color.White.copy(alpha = 0.75f)
        temp >= 42f -> Color(0xFFFF453A)
        temp >= 38f -> Tints.Orange
        else -> Tints.Green
    }
    val pctLabel = if (d.batteryPct >= 0) "${d.batteryPct}%${if (d.isCharging) " ⚡" else ""} · " else ""
    val tempLabel = if (temp == null) "-" else String.format(Lang.locale, "%.1f°C", temp)

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeterRow(tr("Память", "RAM", "RAM"), d.ramUsed, d.ramTotal, Tints.Blue)
        MeterRow(tr("Накопитель", "Storage", "Penyimpanan"), d.diskUsed, d.diskTotal, Tints.Green)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                tr("Батарея ", "Battery ", "Baterai ") + pctLabel + tempLabel,
                color = tempColor, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            )
            Text(
                tr("Нагрев ", "Thermal ", "Termal ") + (d.thermal ?: "-"),
                color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1,
            )
        }
    }
}

@Composable
private fun MeterRow(label: String, used: Long, total: Long, color: Color) {
    val fraction = if (total > 0L) (used.toFloat() / total.toFloat()).coerceIn(0f, 1f) else 0f
    val pct = (fraction * 100).toInt()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$label ($pct%)", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, maxLines = 1)
            Text(
                NetSpeed.bytesText(used, Lang.locale) + " / " + NetSpeed.bytesText(total, Lang.locale),
                color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1,
            )
        }
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.12f))) {
            Box(Modifier.fillMaxWidth(fraction).height(5.dp).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}
