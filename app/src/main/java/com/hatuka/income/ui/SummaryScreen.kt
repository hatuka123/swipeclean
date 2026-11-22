package com.hatuka.income.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatuka.income.data.Fmt
import com.hatuka.income.data.Period
import com.hatuka.income.data.Store
import com.hatuka.income.data.Totals
import java.time.YearMonth

@Composable
fun SummaryScreen() {
    var mode by rememberSaveable { mutableIntStateOf(0) } // 0 month, 1 year, 2 all time
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthKey)

    val period: Period? = when (mode) {
        0 -> Period.month(month)
        1 -> Period.year(month.year)
        else -> null
    }
    val perJob = Totals.perJob(Store.jobs, Store.entries, Store.oneTimes, period)
    val looseOneTime = Store.oneTimes.filter { it.jobId == null && (period == null || period.has(it.day)) }.sumOf { it.amount }
    val grand = Totals.grand(Store.entries, Store.oneTimes, period)
    val allOneTime = Totals.oneTime(Store.oneTimes, period)
    val days = perJob.sumOf { it.days }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Segmented(listOf("חודש", "שנה", "הכול"), mode, { mode = it })
        if (mode != 2) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                // RTL: the first child sits on the right = "back in time".
                TextButton(onClick = { monthKey = (if (mode == 0) month.minusMonths(1) else month.minusYears(1)).toString() }) {
                    Text("▶", fontSize = 20.sp, color = Green)
                }
                Text(
                    if (mode == 0) Fmt.month(month) else "שנת ${month.year}",
                    fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { monthKey = (if (mode == 0) month.plusMonths(1) else month.plusYears(1)).toString() }) {
                    Text("◀", fontSize = 20.sp, color = Green)
                }
            }
        }

        // Big total
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Green).padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("סך כל ההכנסות", color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp)
            Text(Fmt.money(grand), color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            Text(
                (if (days == 1) "יום עבודה אחד" else "$days ימי עבודה") + if (allOneTime > 0) " · מתוכם חד־פעמי ${Fmt.money(allOneTime)}" else "",
                color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp,
            )
        }

        if (Store.jobs.isNotEmpty()) {
            SectionCard("לפי מקום עבודה") {
                perJob.sortedByDescending { it.amount + it.oneTime }.forEach { t ->
                    val total = t.amount + t.oneTime
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Dot(jobColor(t.job.color))
                            Spacer(Modifier.width(8.dp))
                            Text(t.job.name, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
                            Text(Fmt.money(total), fontWeight = FontWeight.Bold, color = TextMain, fontSize = 17.sp)
                        }
                        Bar(if (grand > 0) (total / grand).toFloat() else 0f, jobColor(t.job.color))
                        Text(
                            (if (t.job.kind == com.hatuka.income.data.JobKind.EMPLOYEE) "שכירה" else "עצמאית") +
                                " · ${daysText(t.days)}" +
                                (if (t.days > 0) " · ממוצע ${Fmt.money(t.amount / t.days)} ליום" else "") +
                                (if (t.oneTime > 0) " · כולל ${Fmt.money(t.oneTime)} חד־פעמי" else ""),
                            color = TextDim, fontSize = 13.sp,
                        )
                    }
                }
                if (looseOneTime > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("⭐ הכנסות חד־פעמיות אחרות", color = TextMain, modifier = Modifier.weight(1f))
                        Text(Fmt.money(looseOneTime), fontWeight = FontWeight.Bold, color = TextMain, fontSize = 17.sp)
                    }
                    Bar(if (grand > 0) (looseOneTime / grand).toFloat() else 0f, Gold)
                }
            }
        }

        if (mode != 0) {
            val months = Totals.byMonth(Store.entries, Store.oneTimes, period)
            if (months.isNotEmpty()) {
                SectionCard("לפי חודשים") {
                    val max = months.maxOf { it.second }.coerceAtLeast(1.0)
                    months.forEach { (m, v) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(Fmt.month(m), color = TextMain, modifier = Modifier.width(110.dp), fontSize = 14.sp)
                            Box(Modifier.weight(1f)) { Bar((v / max).toFloat(), Green.copy(alpha = 0.55f)) }
                            Spacer(Modifier.width(8.dp))
                            Text(Fmt.money(v), color = TextMain, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
        }

        if (grand == 0.0) {
            Text("אין הכנסות בתקופה הזאת.", color = TextDim, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun Bar(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(CardSoft)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
    }
}
