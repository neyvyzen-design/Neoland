package com.neoland.app.island

import java.util.Locale

/** 1.8.0: чистые функции монитора сети — скорость из прироста счётчиков и подписи единиц. */
object NetSpeed {
    /** Бит в секунду по приросту байтов за dtMs миллисекунд; отрицательный прирост (сброс счётчика) = 0 */
    fun bitsPerSec(deltaBytes: Long, dtMs: Long): Double =
        if (deltaBytes <= 0L || dtMs <= 0L) 0.0 else deltaBytes * 8000.0 / dtMs

    /** «850 Kbps», «12,8 Mbps» */
    fun speedText(bps: Double, locale: Locale): String = when {
        bps < 1_000.0 -> "0 Kbps"
        bps < 1_000_000.0 -> String.format(locale, "%.0f Kbps", bps / 1_000.0)
        else -> String.format(locale, "%.1f Mbps", bps / 1_000_000.0)
    }

    /** «120 KB», «34,5 MB», «1,20 GB» */
    fun bytesText(bytes: Long, locale: Locale): String = when {
        bytes < 1_000L -> "0 KB"
        bytes < 1_000_000L -> String.format(locale, "%d KB", bytes / 1_000L)
        bytes < 1_000_000_000L -> String.format(locale, "%.1f MB", bytes / 1_000_000.0)
        else -> String.format(locale, "%.2f GB", bytes / 1_000_000_000.0)
    }
}
