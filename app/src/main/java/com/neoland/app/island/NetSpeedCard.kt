package com.neoland.app.island

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.IslandBus
import com.neoland.app.core.Lang
import com.neoland.app.core.Tints
import com.neoland.app.core.tr
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Nayland 2.0: Monitor kecepatan jaringan live (bebas lonjakan negatif VPN, grafik ganda Unduh & Unggah, serta Peak).
 */
object NetLive {
    val down = mutableStateOf(0.0)
    val up = mutableStateOf(0.0)
    val peakDown = mutableStateOf(0.0)
    val used = mutableStateOf(0L)
    val history = mutableStateOf(listOf<Float>())
    val upHistory = mutableStateOf(listOf<Float>())
    val supported = mutableStateOf(true)
    val link = mutableStateOf("")
    private var job: Job? = null

    fun start(context: Context) {
        val appCtx = context.applicationContext
        if (job?.isActive == true) return
        down.value = 0.0
        up.value = 0.0
        peakDown.value = 0.0
        used.value = 0L
        history.value = emptyList()
        upHistory.value = emptyList()
        supported.value = true
        link.value = linkType(appCtx)
        job = IslandBus.scope.launch {
            var rx0 = TrafficStats.getTotalRxBytes()
            var tx0 = TrafficStats.getTotalTxBytes()
            if (rx0 == TrafficStats.UNSUPPORTED.toLong() || tx0 == TrafficStats.UNSUPPORTED.toLong()) {
                supported.value = false
                return@launch
            }
            var totalUsed = 0L
            var t0 = SystemClock.elapsedRealtime()
            var tick = 0
            while (true) {
                delay(1000)
                val t1 = SystemClock.elapsedRealtime()
                val rx1 = TrafficStats.getTotalRxBytes()
                val tx1 = TrafficStats.getTotalTxBytes()
                if (!IslandBus.screenOn.value) {
                    rx0 = rx1
                    tx0 = tx1
                    t0 = t1
                    continue
                }
                val dt = (t1 - t0).coerceAtLeast(1L)
                val dRx = (rx1 - rx0).coerceAtLeast(0L)
                val dTx = (tx1 - tx0).coerceAtLeast(0L)
                val dSpeed = NetSpeed.bitsPerSec(dRx, dt).coerceAtLeast(0.0)
                val uSpeed = NetSpeed.bitsPerSec(dTx, dt).coerceAtLeast(0.0)
                down.value = dSpeed
                up.value = uSpeed
                if (dSpeed > peakDown.value) peakDown.value = dSpeed
                totalUsed += dRx + dTx
                used.value = totalUsed
                history.value = (history.value + dSpeed.toFloat()).takeLast(32)
                upHistory.value = (upHistory.value + uSpeed.toFloat()).takeLast(32)
                rx0 = rx1
                tx0 = tx1
                t0 = t1
                tick++
                if (tick % 4 == 0) link.value = linkType(appCtx)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}

@Composable
fun NetSpeedCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { NetLive.start(context.applicationContext) }
    val down = NetLive.down.value
    val up = NetLive.up.value
    val peak = NetLive.peakDown.value
    val used = NetLive.used.value
    val history = NetLive.history.value
    val upHistory = NetLive.upHistory.value
    val link = NetLive.link.value

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (!NetLive.supported.value) {
            Text(
                tr("Эта прошивка не отдаёт счётчики трафика", "This device does not report traffic counters", "Perangkat ini tidak menyediakan data lalu lintas"),
                color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp,
            )
            return@Column
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(tr("ЗАГРУЗКА", "DOWNLOAD", "UNDUH"), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(NetSpeed.speedText(down, Lang.locale), color = Tints.Green, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            Column(Modifier.weight(1f)) {
                Text(tr("ОТДАЧА", "UPLOAD", "UNGGAH"), color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                Text(NetSpeed.speedText(up, Lang.locale), color = Tints.Blue, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
        }
        Canvas(Modifier.fillMaxWidth().height(42.dp)) {
            val pts = history
            if (pts.size < 2 || size.width <= 0f || size.height <= 0f) return@Canvas
            val maxDown = pts.maxOrNull() ?: 0f
            val maxUp = upHistory.maxOrNull() ?: 0f
            val ceiling = maxOf(maxDown, maxUp, 500_000f)
            val stepX = size.width / (pts.size - 1)

            val downPath = Path()
            val fillPath = Path()
            pts.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v / ceiling).coerceIn(0f, 1f) * size.height
                if (i == 0) {
                    downPath.moveTo(x, y)
                    fillPath.moveTo(x, size.height)
                    fillPath.lineTo(x, y)
                } else {
                    downPath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
            }
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(
                fillPath,
                Brush.verticalGradient(listOf(Tints.Green.copy(alpha = 0.25f), Color.Transparent)),
            )
            drawPath(downPath, Tints.Green, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))

            if (upHistory.size >= 2) {
                val upPath = Path()
                val upStepX = size.width / (upHistory.size - 1)
                upHistory.forEachIndexed { i, v ->
                    val x = i * upStepX
                    val y = size.height - (v / ceiling).coerceIn(0f, 1f) * size.height
                    if (i == 0) upPath.moveTo(x, y) else upPath.lineTo(x, y)
                }
                drawPath(upPath, Tints.Blue.copy(alpha = 0.85f), style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "$link · Peak " + NetSpeed.speedText(peak, Lang.locale),
                color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1,
            )
            Text(
                tr("Исп. ", "Used ", "Terpakai ") + NetSpeed.bytesText(used, Lang.locale),
                color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 1,
            )
        }
    }
}

private fun linkType(context: Context): String {
    return runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
        when {
            caps == null -> tr("Нет сети", "Offline", "Offline")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> tr("Мобильная сеть", "Mobile data", "Data seluler")
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> "VPN"
            else -> tr("Подключено", "Connected", "Terhubung")
        }
    }.getOrDefault(tr("Подключено", "Connected", "Terhubung"))
}
