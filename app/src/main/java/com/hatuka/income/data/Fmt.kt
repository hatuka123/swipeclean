package com.hatuka.income.data

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.YearMonth
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object Fmt {
    private val sym = DecimalFormatSymbols(Locale.US)
    private val whole = DecimalFormat("#,##0", sym)
    private val cents = DecimalFormat("#,##0.00", sym)
    private val plain = DecimalFormat("0.##", sym)

    val months = listOf("ינואר", "פברואר", "מרץ", "אפריל", "מאי", "יוני", "יולי", "אוגוסט", "ספטמבר", "אוקטובר", "נובמבר", "דצמבר")

    /** "₪12,345" (agorot only when there are any). */
    fun money(v: Double): String {
        val s = if (abs(v - v.roundToLong()) < 0.005) whole.format(v.roundToLong()) else cents.format(v)
        return "₪$s"
    }

    /** Rounded to whole shekels, for estimates. */
    fun moneyRound(v: Double) = "₪" + whole.format(v.roundToLong())

    fun pct(rate: Double) = plain.format(rate * 100) + "%"

    fun num(v: Double) = plain.format(v)

    fun date(day: Long): String {
        val d = day.toDate()
        return "%02d.%02d.%02d".format(Locale.US, d.dayOfMonth, d.monthValue, d.year % 100)
    }

    fun range(from: Long, to: Long) = if (from == to) date(from) else "${date(from)} – ${date(to)}"

    fun month(m: YearMonth) = "${months[m.monthValue - 1]} ${m.year}"

    /** Parses what was typed in a money/number box; accepts "," as the decimal point. */
    fun parse(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()

    fun input(v: Double?): String = v?.let { plain.format(it) } ?: ""
}
