package com.neoland.app.island

import java.math.BigDecimal
import java.math.MathContext

/** Движок калькулятора для карточки острова: цифры, + − × ÷, %, ±, C, стирание. Без зависимостей от Android. */
class CalcEngine {
    var display: String = "0"
        private set
    var expr: String = ""
        private set
    private var acc: Double? = null
    private var op: Char? = null
    private var fresh = true
    private var error = false

    private fun reset() { display = "0"; expr = ""; acc = null; op = null; fresh = true; error = false }

    fun clear() = reset()

    fun digit(d: Char) {
        if (error) reset()
        if (fresh) {
            display = d.toString()
        } else if (display == "0") {
            display = d.toString()
        } else if (display.count { it.isDigit() } < 12) {
            display += d
        }
        fresh = false
    }

    fun dot() {
        if (error) reset()
        if (fresh) { display = "0."; fresh = false; return }
        if ('.' !in display) display += "."
    }

    fun op(c: Char) {
        if (error) return
        val cur = display.toDoubleOrNull() ?: return
        val pending = op
        val a = acc
        if (pending != null && a != null && !fresh) {
            val r = apply(a, pending, cur) ?: return fail()
            acc = r
            display = format(r)
        } else {
            acc = cur
        }
        op = c
        expr = "${format(acc ?: cur)} $c"
        fresh = true
    }

    fun equals() {
        if (error) return
        val pending = op ?: return
        val a = acc ?: return
        val b = display.toDoubleOrNull() ?: return
        val r = apply(a, pending, b) ?: return fail()
        expr = "${format(a)} $pending ${format(b)} ="
        display = format(r)
        acc = null
        op = null
        fresh = true
    }

    fun negate() {
        if (error || display == "0") return
        display = if (display.startsWith("-")) display.drop(1) else "-$display"
    }

    fun percent() {
        if (error) return
        val v = display.toDoubleOrNull() ?: return
        display = format(v / 100.0)
        fresh = true
    }

    fun backspace() {
        if (error) { reset(); return }
        if (fresh) return
        display = if (display.length > 1 && !(display.length == 2 && display.startsWith("-"))) display.dropLast(1) else "0"
    }

    private fun fail() {
        reset()
        display = "Error"
        error = true
    }

    private fun apply(a: Double, o: Char, b: Double): Double? = when (o) {
        '+' -> a + b
        '−', '-' -> a - b
        '×', '*' -> a * b
        '÷', '/' -> if (b == 0.0) null else a / b
        else -> null
    }?.takeIf { it.isFinite() }

    private fun format(v: Double): String {
        if (v == 0.0) return "0"
        if (v == Math.rint(v) && Math.abs(v) < 1e15) return v.toLong().toString()
        return BigDecimal(v).round(MathContext(10)).stripTrailingZeros().toPlainString()
    }
}
