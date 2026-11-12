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
 * 1.7.17: pembacaan kecepatan jaringan dipindah ke sini, supaya kapsul kecil dan kartu besar memakai data yang sama.
 * Berjalan sekali per detik hanya selama aktivitas Kecepatan ada dan layar menyala; ditutup -> berhenti.
 */
object NetLive {
    val down = mutableStateOf(0.0)
    val up = mutableStateOf(0.0)
    val used = mutableStateOf(0L)
    val history = mutableStateOf(listOf<Float>())
    val supported = mutableStateOf(true)
    val link = mutableStateOf("")
    private var job: Job? = null

    fun start(context: Context) {
        if (job?.isActive == true) return
        down.value = 0.0
        up.value = 0.0
        used.value = 0L
        history.value = emptyList()
        supported.value = true
        link.value = linkType(context)
        job = IslandBus.scope.launch {
            var rx0 = TrafficStats.getTotalRxBytes()
            var tx0 = TrafficStats.getTotalTxBytes()
            if (rx0 == TrafficStats.UNSUPPORTED.toLong() || tx0 == TrafficStats.UNSUPPORTED.toLong()) {
                supported.value = false
                return@launch
            }
            val startRx = rx0
            val startTx = tx0
            var t0 = SystemClock.elapsedRealtime()
            var tick = 0
            while (true) {
                delay(1000)
                val t1 = SystemClock.elapsedRealtime()
                val rx1 = TrafficStats.getTotalRxBytes()
                val tx1 = TrafficStats.getTotalTxBytes()
                if (!IslandBus.screenOn.value) {
                    // Layar mati: jangan menghitung, supaya tidak ada lonjakan palsu saat layar menyala lagi
                    rx0 = rx1
                    tx0 = tx1
                    t0 = t1
                    continue
                }
                val dt = t1 - t0
                down.value = NetSpeed.bitsPerSec(rx1 - rx0, dt)
                up.value = NetSpeed.bitsPerSec(tx1 - tx0, dt)
                used.value = (rx1 - startRx).coerceAtLeast(0L) + (tx1 - startTx).coerceAtLeast(0L)
                history.value = (history.value + down.value.toFloat()).takeLast(30)
                rx0 = rx1
                tx0 = tx1
                t0 = t1
                tick++
                if (tick % 5 == 0) link.value = linkType(context)
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
    // Tidak berbuat apa-apa kalau pembacaan sudah jalan (dimulai saat aktivitas dibuka)
    LaunchedEffect(Unit) { NetLive.start(context.applicationContext) }
    val down = NetLive.down.value
    val up = NetLive.up.value
    val used = NetLive.used.value
    val history = NetLive.history.value
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
        val line = Tints.Green
        Canvas(Modifier.fillMaxWidth().height(36.dp)) {
            val pts = history
            if (pts.size < 2) return@Canvas
            val peak = (pts.maxOrNull() ?: 0f).coerceAtLeast(1_000_000f)
            val stepX = size.width / (pts.size - 1)
            val path = Path()
            pts.forEachIndexed { i, v ->
                val x = i * stepX
                val y = size.height - (v / peak) * size.height
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, line, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(link, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1)
            Text(
                tr("Использовано ", "Used ", "Terpakai ") + NetSpeed.bytesText(used, Lang.locale),
                color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, maxLines = 1,
            )
        }
    }
}

/** Jenis koneksi: Wi-Fi, data seluler, atau offline */
private fun linkType(context: Context): String {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
    return when {
        caps == null -> tr("Нет сети", "Offline", "Offline")
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> tr("Мобильная сеть", "Mobile data", "Data seluler")
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
        else -> tr("Подключено", "Connected", "Terhubung")
    }
}
