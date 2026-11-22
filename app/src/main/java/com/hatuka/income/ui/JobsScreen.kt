package com.hatuka.income.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hatuka.income.data.Fmt
import com.hatuka.income.data.Job
import com.hatuka.income.data.JobKind
import com.hatuka.income.data.Period
import com.hatuka.income.data.Store
import com.hatuka.income.data.Totals
import java.time.LocalDate

@Composable
fun JobsScreen() {
    var editing by remember { mutableStateOf<Job?>(null) }
    val year = LocalDate.now().year
    val totals = Totals.perJob(Store.jobs, Store.entries, Store.oneTimes, Period.year(year)).associateBy { it.job.id }
    val context = LocalContext.current

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = { editing = Job(0, "", JobKind.EMPLOYEE, color = Store.jobs.size) },
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text("＋ הוספת מקום עבודה", fontSize = 18.sp, fontWeight = FontWeight.Bold) }

        if (Store.jobs.isEmpty()) {
            SectionCard("ברוכה הבאה! 👋") {
                Text(
                    "כאן מוסיפים את כל המקומות שבהם את עובדת.\n" +
                        "לכל מקום מסמנים אם את שכירה או עצמאית, ואם את שכירה – אפשר לשמור את הקישור למערכת השכר (תלושים).\n\n" +
                        "אחרי זה, בלשונית \"רישום\" רושמים מתי עבדת וכמה כסף נכנס, וב\"סיכום\" רואים כמה הרווחת.",
                    color = TextMain, fontSize = 15.sp,
                )
            }
        }

        Store.jobs.forEach { job ->
            val t = totals[job.id]
            SectionCard(null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Dot(jobColor(job.color), 18)
                    Spacer(Modifier.width(10.dp))
                    Text(job.name, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f))
                    KindBadge(job.kind)
                    IconButton(onClick = { editing = job }) { Icon(Icons.Filled.Edit, "עריכה", tint = TextDim) }
                }
                if (t != null) {
                    Text(
                        "השנה ($year): ${Fmt.money(t.amount + t.oneTime)} · ${t.days} ימי עבודה",
                        color = TextDim, fontSize = 14.sp,
                    )
                }
                if (job.kind == JobKind.EMPLOYEE) {
                    if (job.payrollUrl.isNotBlank()) {
                        OutlinedButton(onClick = { openLink(context, job.payrollUrl) }, modifier = Modifier.fillMaxWidth()) {
                            Text("🔗 כניסה למערכת השכר", fontSize = 16.sp)
                        }
                    } else {
                        TextButton(onClick = { editing = job }) { Text("＋ הוספת קישור למערכת השכר", color = Green) }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
    }

    editing?.let { JobDialog(it, onDismiss = { editing = null }) }
}

@Composable
fun KindBadge(kind: JobKind) {
    if (kind == JobKind.EMPLOYEE) Badge("שכירה", Green, GreenSoft) else Badge("עצמאית", Color(0xFF8A5A10), GoldSoft)
}

@Composable
private fun JobDialog(job: Job, onDismiss: () -> Unit) {
    val isNew = job.id == 0L
    var name by remember { mutableStateOf(job.name) }
    var kind by remember { mutableStateOf(job.kind) }
    var url by remember { mutableStateOf(job.payrollUrl) }
    var color by remember { mutableStateOf(job.color) }
    var confirmDelete by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "מקום עבודה חדש" else "עריכת מקום עבודה") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TextInput("שם מקום העבודה", name, { name = it })
                Text("אני במקום הזה:", color = TextDim)
                Segmented(listOf("שכירה", "עצמאית"), if (kind == JobKind.EMPLOYEE) 0 else 1, { kind = if (it == 0) JobKind.EMPLOYEE else JobKind.SELF })
                if (kind == JobKind.EMPLOYEE) {
                    TextInput("קישור למערכת השכר (לא חובה)", url, { url = it.trim() }, keyboard = KeyboardType.Uri)
                    Text("למשל הקישור לאתר שבו רואים את התלושים (חילן, מיכפל, סינאל…)", color = TextDim, fontSize = 12.sp)
                } else {
                    Text("כעצמאית לא מנכים לך מס מהתשלום – בלשונית \"מס\" יש הסבר מה זה אומר.", color = TextDim, fontSize = 13.sp)
                }
                Text("צבע:", color = TextDim)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    JobColors.indices.forEach { i ->
                        Box(
                            Modifier
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(jobColor(i))
                                .border(if (i == color) 3.dp else 0.dp, TextMain, CircleShape)
                                .clickable { color = i }
                        )
                    }
                }
                if (!isNew) {
                    TextButton(onClick = { confirmDelete = true }) { Text("🗑 מחיקת מקום העבודה", color = Bad) }
                }
            }
        },
        confirmButton = {
            Button(enabled = name.isNotBlank(), onClick = {
                Store.saveJob(job.copy(name = name.trim(), kind = kind, payrollUrl = if (kind == JobKind.EMPLOYEE) url else "", color = color))
                onDismiss()
            }) { Text("שמירה") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ביטול") } },
    )

    if (confirmDelete) {
        val n = Store.entries.count { it.jobId == job.id }
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("למחוק את \"${job.name}\"?") },
            text = { Text(if (n > 0) "יימחקו גם $n רישומי עבודה של המקום הזה. אי אפשר לבטל." else "אי אפשר לבטל.") },
            confirmButton = {
                Button(onClick = { Store.deleteJob(job.id); confirmDelete = false; onDismiss() }, colors = ButtonDefaults.buttonColors(containerColor = Bad)) { Text("מחיקה") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("ביטול") } },
        )
    }
}
