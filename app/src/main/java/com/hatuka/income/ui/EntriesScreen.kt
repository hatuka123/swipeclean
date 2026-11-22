package com.hatuka.income.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatuka.income.data.Fmt
import com.hatuka.income.data.OneTime
import com.hatuka.income.data.Store
import com.hatuka.income.data.WorkEntry
import com.hatuka.income.data.toMonth
import com.hatuka.income.data.today

private sealed interface Item {
    val day: Long
    data class Work(val e: WorkEntry) : Item { override val day get() = e.toDay }
    data class Once(val o: OneTime) : Item { override val day get() = o.day }
}

@Composable
fun EntriesScreen(onGoJobs: () -> Unit) {
    var editWork by remember { mutableStateOf<WorkEntry?>(null) }
    var editOnce by remember { mutableStateOf<OneTime?>(null) }
    var filter by remember { mutableStateOf<Long?>(null) } // job id, -1 = one-offs only

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (Store.jobs.isEmpty()) {
                SectionCard("קודם מוסיפים מקום עבודה") {
                    Text("כדי לרשום ימי עבודה צריך מקום עבודה אחד לפחות.", color = TextMain)
                    Button(onClick = onGoJobs) { Text("למקומות העבודה") }
                }
            } else {
                Button(
                    onClick = {
                        val last = Store.entries.maxByOrNull { it.id }
                        editWork = WorkEntry(0, last?.jobId ?: Store.jobs.first().id, today(), today(), 1, 0.0)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text("＋ רישום עבודה (ימים וכסף)", fontSize = 17.sp, fontWeight = FontWeight.Bold) }
            }
            OutlinedButton(
                onClick = { editOnce = OneTime(0, today(), 0.0, "") },
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) { Text("⭐ הכנסה חד־פעמית", fontSize = 16.sp) }

            if (Store.jobs.size > 1 || Store.oneTimes.isNotEmpty()) {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("הכול", null, filter == null) { filter = null }
                    Store.jobs.forEach { j -> Chip(j.name, jobColor(j.color), filter == j.id) { filter = j.id } }
                    if (Store.oneTimes.isNotEmpty()) Chip("⭐ חד־פעמי", Gold, filter == -1L) { filter = -1L }
                }
            }
        }

        val rows: List<Item> = buildList<Item> {
            if (filter != -1L) addAll(Store.entries.filter { filter == null || it.jobId == filter }.map { Item.Work(it) })
            if (filter == null || filter == -1L) addAll(Store.oneTimes.map { Item.Once(it) })
        }.sortedWith(compareByDescending<Item> { it.day }.thenByDescending { if (it is Item.Work) it.e.id else (it as Item.Once).o.id })

        if (rows.isEmpty() && Store.jobs.isNotEmpty()) {
            Text(
                "עוד אין רישומים. לחצי על \"רישום עבודה\" ותרשמי מתי עבדת, כמה ימים וכמה כסף נכנס.",
                color = TextDim, modifier = Modifier.padding(20.dp), fontSize = 15.sp,
            )
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val grouped = rows.groupBy { it.day.toMonth() }
            grouped.forEach { (month, list) ->
                item(key = "m$month") {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(Fmt.month(month), color = Green, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                        Text(Fmt.money(list.sumOf { if (it is Item.Work) it.e.amount else (it as Item.Once).o.amount }), color = Green, fontWeight = FontWeight.Bold)
                    }
                }
                items(list, key = { if (it is Item.Work) "w${it.e.id}" else "o${(it as Item.Once).o.id}" }) { r ->
                    when (r) {
                        is Item.Work -> WorkRow(r.e) { editWork = r.e }
                        is Item.Once -> OnceRow(r.o) { editOnce = r.o }
                    }
                }
            }
            item { Spacer(Modifier.height(30.dp)) }
        }
    }

    editWork?.let { WorkDialog(it) { editWork = null } }
    editOnce?.let { OnceDialog(it) { editOnce = null } }
}

@Composable
private fun Chip(label: String, color: Color?, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (on) Green else Card)
            .border(1.dp, Line, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) { Dot(color, 10); Spacer(Modifier.width(6.dp)) }
        Text(label, color = if (on) Color.White else TextMain, fontSize = 14.sp)
    }
}

@Composable
private fun ItemCard(stripe: Color, onClick: () -> Unit, content: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .heightIn(min = 64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Card)
            .border(1.dp, Line, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box(Modifier.width(6.dp).fillMaxHeight().background(stripe))
        Box(Modifier.padding(10.dp)) { content() }
    }
}

@Composable
private fun WorkRow(e: WorkEntry, onClick: () -> Unit) {
    val job = Store.job(e.jobId)
    ItemCard(jobColor(job?.color ?: 0), onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(job?.name ?: "?", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
                Text("${Fmt.range(e.fromDay, e.toDay)} · ${daysText(e.days)}", color = TextDim, fontSize = 13.sp)
                if (e.note.isNotBlank()) Text(e.note, color = TextDim, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
            Text(Fmt.money(e.amount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMain)
        }
    }
}

@Composable
private fun OnceRow(o: OneTime, onClick: () -> Unit) {
    val job = Store.job(o.jobId)
    ItemCard(Gold, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("⭐ " + o.title.ifBlank { "הכנסה חד־פעמית" }, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextMain)
                Text(Fmt.date(o.day) + (job?.let { " · ${it.name}" } ?: ""), color = TextDim, fontSize = 13.sp)
            }
            Text(Fmt.money(o.amount), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextMain)
        }
    }
}

fun daysText(n: Int) = if (n == 1) "יום אחד" else "$n ימים"

@Composable
private fun JobPicker(selected: Long?, onSelect: (Long?) -> Unit, allowNone: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (allowNone) PickRow("לא קשור לעבודה מסוימת", null, selected == null) { onSelect(null) }
        Store.jobs.forEach { j -> PickRow(j.name, jobColor(j.color), selected == j.id) { onSelect(j.id) } }
    }
}

@Composable
private fun PickRow(label: String, color: Color?, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (on) GreenSoft else CardSoft)
            .border(if (on) 2.dp else 0.dp, Green, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (color != null) { Dot(color); Spacer(Modifier.width(8.dp)) }
        Text(label, color = TextMain, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal, modifier = Modifier.weight(1f))
        if (on) Text("✓", color = Green, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WorkDialog(entry: WorkEntry, onDismiss: () -> Unit) {
    val isNew = entry.id == 0L
    var jobId by remember { mutableStateOf(entry.jobId) }
    var from by remember { mutableStateOf(entry.fromDay) }
    var to by remember { mutableStateOf(entry.toDay) }
    var daysStr by remember { mutableStateOf(entry.days.toString()) }
    // Days follow the dates until they are typed by hand.
    var daysTouched by remember { mutableStateOf(!isNew && entry.days.toLong() != entry.toDay - entry.fromDay + 1) }
    var amount by remember { mutableStateOf(if (isNew) "" else Fmt.input(entry.amount)) }
    var note by remember { mutableStateOf(entry.note) }
    var confirmDelete by remember { mutableStateOf(false) }

    fun syncDays() { if (!daysTouched) daysStr = (to - from + 1).coerceAtLeast(1).toString() }

    val amt = Fmt.parse(amount)
    val days = daysStr.toIntOrNull()
    val ok = amt != null && amt >= 0 && days != null && Store.job(jobId) != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "רישום עבודה" else "עריכת רישום") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("איפה עבדת?", color = TextDim)
                JobPicker(jobId, { if (it != null) jobId = it }, allowNone = false)
                Text("מתי? (יום אחד – אותו תאריך בשניהם)", color = TextDim)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DateButton("מתאריך", from, { from = it; if (to < from) to = from; syncDays() }, Modifier.weight(1f))
                    DateButton("עד תאריך", to, { to = it; if (from > to) from = to; syncDays() }, Modifier.weight(1f))
                }
                NumberField("כמה ימים עבדת", daysStr, { daysStr = it; daysTouched = true }, decimal = false, suffix = "ימים")
                NumberField("כמה כסף נכנס", amount, { amount = it }, suffix = "₪")
                TextInput("הערה (לא חובה)", note, { note = it }, singleLine = false)
                if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text("🗑 מחיקת הרישום", color = Bad) }
            }
        },
        confirmButton = {
            Button(enabled = ok, onClick = {
                Store.saveEntry(entry.copy(jobId = jobId, fromDay = from, toDay = to, days = days ?: 0, amount = amt ?: 0.0, note = note.trim()))
                onDismiss()
            }) { Text("שמירה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )

    if (confirmDelete) DeleteConfirm("למחוק את הרישום?", { Store.deleteEntry(entry.id); onDismiss() }) { confirmDelete = false }
}

@Composable
private fun OnceDialog(o: OneTime, onDismiss: () -> Unit) {
    val isNew = o.id == 0L
    var title by remember { mutableStateOf(o.title) }
    var day by remember { mutableStateOf(o.day) }
    var amount by remember { mutableStateOf(if (isNew) "" else Fmt.input(o.amount)) }
    var jobId by remember { mutableStateOf(o.jobId) }
    var confirmDelete by remember { mutableStateOf(false) }
    val amt = Fmt.parse(amount)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "הכנסה חד־פעמית" else "עריכת הכנסה חד־פעמית") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput("על מה? (מענק, בונוס, עבודה מזדמנת…)", title, { title = it })
                DateButton("תאריך", day, { day = it }, Modifier.fillMaxWidth())
                NumberField("סכום", amount, { amount = it }, suffix = "₪")
                Text("שייך למקום עבודה? (למשל בונוס מהעבודה)", color = TextDim)
                JobPicker(jobId, { jobId = it }, allowNone = true)
                if (!isNew) TextButton(onClick = { confirmDelete = true }) { Text("🗑 מחיקה", color = Bad) }
            }
        },
        confirmButton = {
            Button(enabled = amt != null && amt >= 0, onClick = {
                Store.saveOneTime(o.copy(title = title.trim(), day = day, amount = amt ?: 0.0, jobId = jobId))
                onDismiss()
            }) { Text("שמירה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )

    if (confirmDelete) DeleteConfirm("למחוק את ההכנסה?", { Store.deleteOneTime(o.id); onDismiss() }) { confirmDelete = false }
}

@Composable
fun DeleteConfirm(title: String, onDelete: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text("אי אפשר לבטל.") },
        confirmButton = {
            Button(onClick = { onDelete(); onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Bad)) { Text("מחיקה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )
}
