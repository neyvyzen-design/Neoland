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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import kotlinx.coroutines.delay

/** Снимок состояния телефона: память, накопитель, температура батареи и тепловой статус системы */
data class DeviceSnapshot(
    val ramUsed: Long,
    val ramTotal: Long,
    val diskUsed: Long,
    val diskTotal: Long,
    val batteryTempC: Float?,
    val thermal: String?,
)

object DeviceStats {
    fun read(context: Context): DeviceSnapshot {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val mi = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(mi)
        val stat = runCatching { StatFs(Environment.getDataDirectory().path) }.getOrNull()
        val diskTotal = stat?.totalBytes ?: 0L
        val diskFree = stat?.availableBytes ?: 0L
        val intent: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val rawTemp = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE) ?: Int.MIN_VALUE
        val temp = if (rawTemp == Int.MIN_VALUE) null else rawTemp / 10f
        return DeviceSnapshot(
            ramUsed = (mi.totalMem - mi.availMem).coerceAtLeast(0L),
            ramTotal = mi.totalMem,
            diskUsed = (diskTotal - diskFree).coerceAtLeast(0L),
            diskTotal = diskTotal,
            batteryTempC = temp,
            thermal = thermalLabel(context),
        )
    }

    private fun thermalLabel(context: Context): String? {
        if (Build.VERSION.SDK_INT < 29) return null
        val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return null
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> tr("Норма", "Normal", "Normal")
            PowerManager.THERMAL_STATUS_LIGHT -> tr("Тепло", "Light", "Hangat")
            PowerManager.THERMAL_STATUS_MODERATE -> tr("Горячо", "Moderate", "Panas")
            PowerManager.THERMAL_STATUS_SEVERE -> tr("Очень горячо", "Severe", "Sangat panas")
            else -> tr("Критично", "Critical", "Kritis")
        }
    }
}

/**
 * 1.7.9: карточка «Устройство» в раскрытом пустом острове. Данные читаются раз в две секунды,
 * только пока карточка на экране.
 */
@Composable
fun DeviceCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val d = DeviceLive.snap.value ?: DeviceStats.read(context)
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MeterRow(tr("Память", "RAM", "RAM"), d.ramUsed, d.ramTotal, Tints.Blue)
        MeterRow(tr("Накопитель", "Storage", "Penyimpanan"), d.diskUsed, d.diskTotal, Tints.Green)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            val temp = d.batteryTempC
            Text(
                tr("Батарея ", "Battery ", "Baterai ") + (if (temp == null) "-" else String.format(Lang.locale, "%.1f°C", temp)),
                color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp, maxLines = 1,
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
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1)
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
