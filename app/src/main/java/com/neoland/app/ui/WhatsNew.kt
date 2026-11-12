package com.neoland.app.ui

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.tr

/**
 * 1.7.14: после обновления при первом запуске приложение показывает, что в нём изменилось.
 * Новые версии дописываются в начало [releases]; показываются только те, что новее последней виденной.
 */
object WhatsNew {
    class Item(val ru: String, val en: String, val id: String)
    class Release(val version: String, val items: List<Item>)

    val releases = listOf(
        Release(
            "1.7.15",
            listOf(
                Item(
                    "Пустой остров: сначала только таймер, остальные действия — отдельными страницами, листаются свайпом",
                    "Empty island: only the timer first; every other option is its own page, swipe to switch",
                    "Island kosong: awalnya hanya timer; tiap opsi lain tampil mandiri, geser untuk berpindah",
                ),
                Item(
                    "Калькулятор, скорость сети и карточка «Устройство» открываются целой страницей",
                    "Calculator, network speed and the Device card open as their own page",
                    "Kalkulator, kecepatan jaringan, dan kartu Perangkat tampil sebagai halaman sendiri",
                ),
                Item(
                    "Горизонтальный режим: положение камеры больше не сбивается сразу после поворота",
                    "Landscape: the camera position no longer goes wrong right after rotating",
                    "Landscape: posisi kamera tidak lagi salah tepat setelah layar diputar",
                ),
            ),
        ),
        Release(
            "1.7.14",
            listOf(
                Item(
                    "Анимация Face ID убрана, осталась анимация замка: он закрывается при блокировке и открывается, когда телефон разблокирован",
                    "Face ID animation removed; only the lock animation remains: the lock closes when locked and opens once the phone is unlocked",
                    "Animasi Face ID dihapus, tersisa animasi gembok: gembok menutup saat terkunci dan terbuka setelah HP dibuka",
                ),
                Item(
                    "После обновления приложение показывает список изменений, как это окно",
                    "After an update the app shows what changed, like this window",
                    "Setelah update, aplikasi menampilkan daftar perubahan seperti jendela ini",
                ),
            ),
        ),
        Release(
            "1.7.12",
            listOf(
                Item(
                    "Горизонтальная ориентация: остров выравнивается по объективу камеры, добавлен ползунок сдвига по высоте",
                    "Landscape: the island lines up with the camera lens, with a vertical shift slider",
                    "Landscape: island sejajar dengan lensa kamera, ada slider geser vertikal",
                ),
            ),
        ),
    )

    private fun parse(v: String): List<Int> = v.split('.').map { part -> part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0 }

    fun compare(a: String, b: String): Int {
        val x = parse(a)
        val y = parse(b)
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 } - y.getOrElse(i) { 0 }
            if (d != 0) return d
        }
        return 0
    }

    /** Версии новее [seen] и не новее [current], от новой к старой, не больше четырёх */
    fun pending(seen: String, current: String): List<Release> =
        releases.filter { compare(it.version, seen) > 0 && compare(it.version, current) <= 0 }
            .sortedWith { a, b -> compare(b.version, a.version) }
            .take(4)
}

@Composable
fun WhatsNewHost() {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<List<WhatsNew.Release>>(emptyList()) }
    var current by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val name = runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
            ?: return@LaunchedEffect
        val seen = context.getSharedPreferences("whats_new", Context.MODE_PRIVATE).getString("seen_version", "") ?: ""
        current = name
        pending = WhatsNew.pending(seen, name)
    }
    if (pending.isNotEmpty()) {
        val close = {
            context.getSharedPreferences("whats_new", Context.MODE_PRIVATE).edit().putString("seen_version", current).apply()
            pending = emptyList()
        }
        AlertDialog(
            onDismissRequest = close,
            confirmButton = { TextButton(onClick = close) { Text("OK") } },
            title = { Text(tr("Что нового", "What's new", "Yang baru") + " · " + pending.first().version) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    pending.forEachIndexed { index, release ->
                        if (index > 0) Spacer(Modifier.height(14.dp))
                        Text("v" + release.version, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Spacer(Modifier.height(6.dp))
                        release.items.forEach { item ->
                            Text("• " + tr(item.ru, item.en, item.id), fontSize = 14.sp)
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            },
        )
    }
}
