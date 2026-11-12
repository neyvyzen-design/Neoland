package com.neoland.app.island

import java.util.Locale

/**
 * 1.7.4: «живой» процент при зарядке. Система отдаёт только целые проценты, поэтому между двумя соседними
 * процентами дробная часть оценивается по времени, которое занял предыдущий процент. Это оценка, а не измерение.
 */
object BatteryEstimator {
    private const val DEFAULT_STEP_MS = 90_000L
    private const val MIN_STEP_MS = 10_000L
    private const val MAX_STEP_MS = 600_000L
    private const val MAX_FRACTION = 0.95f

    private var level = -1
    private var charging = false
    private var since = 0L
    private var sinceReliable = false
    private var stepMs = 0L

    /** Вызывать при каждом обновлении батареи */
    @Synchronized
    fun update(newLevel: Int, newCharging: Boolean, now: Long) {
        if (newCharging != charging || level < 0) {
            charging = newCharging
            level = newLevel
            since = now
            sinceReliable = false
            stepMs = 0L
            return
        }
        if (newLevel == level) return
        val diff = newLevel - level
        if (diff == 1 && sinceReliable) {
            val step = (now - since).coerceIn(MIN_STEP_MS, MAX_STEP_MS)
            stepMs = if (stepMs == 0L) step else (stepMs + step) / 2
        } else if (diff != 1) {
            stepMs = 0L
        }
        level = newLevel
        since = now
        sinceReliable = true
    }

    /** Процент с дробной частью. Без зарядки и на 100% — целый. */
    @Synchronized
    fun precise(shownLevel: Int, isCharging: Boolean, now: Long): Float {
        if (!isCharging || shownLevel < 0 || shownLevel >= 100) return shownLevel.toFloat()
        if (shownLevel != level || !charging) return shownLevel.toFloat()
        val step = if (stepMs > 0L) stepMs else DEFAULT_STEP_MS
        val frac = ((now - since).toFloat() / step).coerceIn(0f, MAX_FRACTION)
        return shownLevel + frac
    }

    /** «57,43%» при зарядке (запятая или точка — по языку), иначе «57%» */
    fun text(p: Float, isCharging: Boolean, locale: Locale): String =
        if (!isCharging || p < 0f || p >= 100f) "${p.toInt()}%" else String.format(locale, "%.2f%%", p)
}
