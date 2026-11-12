package com.neoland.app.island

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import com.neoland.app.core.Tints
import kotlin.math.min

/**
 * Этапы значка на экране блокировки. С версии 1.7.14 анимации Face ID нет: поиск лица и «не узнали» показывают
 * тот же закрытый замок, а «узнали» (OK) — замок, который открывается. Имена этапов оставлены прежними,
 * чтобы остальной код острова не менялся.
 */
enum class FaceStage { NONE, LOCK, SCAN, FAIL, OK }

/** Состояние значка, общее для всех частей острова: пишет его IslandOverlay, читает отрисовка острова */
object FaceState {
    val stage = mutableStateOf(FaceStage.NONE)
    var blankId: Any? = null

    /** Когда последний раз показали «замок открылся» (elapsedRealtime) — чтобы не дублировать значок «замок открыт» */
    @Volatile
    var lastOkAt: Long = -100_000L
}

/** Тайминги прежних этапов (мс); SCAN и FAIL теперь выглядят как обычный замок, OK — открывающийся замок */
const val FACE_SCAN_MS = 3000L
const val FACE_FAIL_MS = 1400L
const val FACE_OK_MS = 1300L

/**
 * Замок на экране блокировки. Появляется на пружине, дужка опускается и закрывается. Когда телефон открыт,
 * замок зеленеет, дужка поднимается и отходит с одной стороны — как открытый замок на iPhone.
 */
@Composable
fun FaceIdGlyph(stage: FaceStage, modifier: Modifier = Modifier) {
    val show = stage != FaceStage.NONE
    val opening = stage == FaceStage.OK
    val alpha by animateFloatAsState(if (show) 1f else 0f, tween(240, easing = FastOutSlowInEasing), label = "lockAlpha")
    val pop by animateFloatAsState(
        if (show) 1f else 0.6f,
        spring(dampingRatio = 0.6f, stiffness = 360f),
        label = "lockPop",
    )
    val close = remember { Animatable(1f) }
    val open = remember { Animatable(0f) }
    LaunchedEffect(show) {
        if (show) {
            close.snapTo(0f)
            close.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 480f))
        }
    }
    LaunchedEffect(opening) {
        if (opening) open.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 320f)) else open.snapTo(0f)
    }
    val green = Tints.Green
    Canvas(modifier) {
        val side = min(size.width, size.height) * 0.78f
        val ox = (size.width - side) / 2f
        val oy = (size.height - side) / 2f
        translate(ox, oy) {
            scale(pop, Offset(side / 2f, side / 2f)) {
                val tint = lerp(Color.White, green, open.value.coerceIn(0f, 1f)).copy(alpha = alpha.coerceIn(0f, 1f))
                drawLock(side, tint, close.value, open.value)
            }
        }
    }
}

/** Замок: корпус и дужка. close 0→1 — дужка опускается; open 0→1 — дужка поднимается и правая ножка отходит */
private fun DrawScope.drawLock(side: Float, color: Color, close: Float, open: Float) {
    val sw = side * 0.075f
    val stroke = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val lift = ((1f - close) * side * 0.16f).coerceAtLeast(-side * 0.02f) + open * side * 0.12f
    val rightEnd = side * 0.48f - open * side * 0.14f
    drawArc(color, 180f, 180f, false, Offset(side * 0.34f, side * 0.18f - lift), Size(side * 0.32f, side * 0.32f), style = stroke)
    drawLine(color, Offset(side * 0.34f, side * 0.34f - lift), Offset(side * 0.34f, side * 0.48f), sw, StrokeCap.Round)
    drawLine(color, Offset(side * 0.66f, side * 0.34f - lift), Offset(side * 0.66f, rightEnd), sw, StrokeCap.Round)
    drawRoundRect(color, Offset(side * 0.24f, side * 0.46f), Size(side * 0.52f, side * 0.38f), CornerRadius(side * 0.07f, side * 0.07f))
}
