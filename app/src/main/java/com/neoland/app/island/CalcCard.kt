package com.neoland.app.island

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.Controls
import com.neoland.app.core.Tints
import com.neoland.app.core.tr
import kotlinx.coroutines.delay

/** Nayland 2.0: Kalkulator Live dengan haptic feedback, salin hasil instan, dan font adaptif. */
@Composable
fun CalcCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptic = rememberHaptic()
    val calc = CalcLive.engine
    val tick = CalcLive.tick
    val copied = remember { mutableStateOf(false) }

    LaunchedEffect(copied.value) {
        if (copied.value) {
            delay(1400)
            copied.value = false
        }
    }

    @Suppress("UNUSED_VARIABLE")
    val version = tick.intValue

    fun press(block: CalcEngine.() -> Unit) {
        haptic()
        calc.block()
        tick.intValue = tick.intValue + 1
    }

    fun tap(k: String) = press {
        when (k) {
            "C" -> clear()
            "\u00B1" -> negate()
            "%" -> percent()
            "=" -> equals()
            "." -> dot()
            "\u00F7", "\u00D7", "\u2212", "+" -> op(k[0])
            else -> digit(k[0])
        }
    }

    val rows = listOf(
        listOf("C", "\u00B1", "%", "\u00F7"),
        listOf("7", "8", "9", "\u00D7"),
        listOf("4", "5", "6", "\u2212"),
        listOf("1", "2", "3", "+"),
    )

    val displaySize = when {
        calc.display.length > 13 -> 21.sp
        calc.display.length > 10 -> 25.sp
        else -> 30.sp
    }

    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(if (copied.value) Tints.Green.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.10f))
                    .clickable {
                        haptic()
                        if (Controls.copyText(context, "Nayland Calc", calc.display)) {
                            copied.value = true
                        }
                    }
                    .padding(horizontal = 9.dp, vertical = 3.dp),
            ) {
                Text(
                    if (copied.value) tr("Скопировано", "Copied", "Tersalin") else tr("Копировать", "Copy", "Salin"),
                    color = if (copied.value) Tints.Green else Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                )
            }
            Text(
                calc.expr,
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
        }
        Text(
            calc.display,
            color = Color.White,
            fontSize = displaySize,
            fontWeight = FontWeight.Light,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().clickable { press { backspace() } },
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k -> CalcKey(k, Modifier.weight(1f)) { tap(k) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey("0", Modifier.weight(1f)) { tap("0") }
            CalcKey(".", Modifier.weight(1f)) { tap(".") }
            CalcKey("\u232B", Modifier.weight(1f), isSecondaryFn = true) { press { backspace() } }
            CalcKey("=", Modifier.weight(1f)) { tap("=") }
        }
    }
}

@Composable
private fun CalcKey(label: String, modifier: Modifier, isSecondaryFn: Boolean = false, onClick: () -> Unit) {
    val isOp = label in listOf("\u00F7", "\u00D7", "\u2212", "+", "=")
    val isFn = isSecondaryFn || label in listOf("C", "\u00B1", "%")
    val bg = when {
        isOp -> Tints.Orange
        isFn -> Color.White.copy(alpha = 0.22f)
        else -> Color.White.copy(alpha = 0.12f)
    }
    Box(
        modifier.height(38.dp).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (isOp) Color.Black else Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}
