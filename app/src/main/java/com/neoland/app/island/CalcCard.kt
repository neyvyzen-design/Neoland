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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neoland.app.core.Tints

/** 1.7.4: карточка калькулятора в раскрытом пустом острове. Нажатие на число стирает последнюю цифру. */
@Composable
fun CalcCard(modifier: Modifier = Modifier) {
    val calc = CalcLive.engine
    val tick = CalcLive.tick
    @Suppress("UNUSED_VARIABLE")
    val version = tick.intValue
    fun press(block: CalcEngine.() -> Unit) {
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
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.06f)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            calc.expr, color = Color.White.copy(alpha = 0.5f), fontSize = 13.sp, maxLines = 1,
            textAlign = TextAlign.End, modifier = Modifier.fillMaxWidth().height(18.dp),
        )
        Text(
            calc.display, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Light, maxLines = 1,
            overflow = TextOverflow.Clip, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().clickable { press { backspace() } },
        )
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k -> CalcKey(k, Modifier.weight(1f)) { tap(k) } }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CalcKey("0", Modifier.weight(2f)) { tap("0") }
            CalcKey(".", Modifier.weight(1f)) { tap(".") }
            CalcKey("=", Modifier.weight(1f)) { tap("=") }
        }
    }
}

@Composable
private fun CalcKey(label: String, modifier: Modifier, onClick: () -> Unit) {
    val isOp = label in listOf("\u00F7", "\u00D7", "\u2212", "+", "=")
    val isFn = label in listOf("C", "\u00B1", "%")
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
