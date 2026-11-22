package com.hatuka.income.ui

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.hatuka.income.data.CrashLog
import com.hatuka.income.data.Fmt

private const val DAY_MS = 86_400_000L

@Composable
fun SectionCard(title: String?, modifier: Modifier = Modifier, bg: Color = Card, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, Line, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (title != null) Text(title, color = Green, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        content()
    }
}

/** Two or three side-by-side choices. */
@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, Line, RoundedCornerShape(12.dp))
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .background(if (on) Green else CardSoft)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) Color.White else TextMain, fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}

@Composable
fun Dot(color: Color, size: Int = 14) {
    Box(Modifier.size(size.dp).clip(CircleShape).background(color))
}

@Composable
fun Badge(text: String, fg: Color, bg: Color) {
    Text(
        text,
        color = fg,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(bg).padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** A number box (money, days, points). Keeps the raw text so typing "12." works. */
@Composable
fun NumberField(label: String, text: String, onText: (String) -> Unit, modifier: Modifier = Modifier, suffix: String? = null, decimal: Boolean = true) {
    OutlinedTextField(
        value = text,
        onValueChange = { t -> onText(t.filter { it.isDigit() || (decimal && (it == '.' || it == ',')) }) },
        label = { Text(label) },
        suffix = suffix?.let { { Text(it, color = TextDim) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
fun TextInput(label: String, text: String, onText: (String) -> Unit, modifier: Modifier = Modifier, keyboard: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    OutlinedTextField(
        value = text,
        onValueChange = onText,
        label = { Text(label) },
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        modifier = modifier.fillMaxWidth(),
    )
}

/** A button that shows a date and opens the calendar. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateButton(label: String, day: Long, onPick: (Long) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    OutlinedButton(onClick = { open = true }, modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 12.sp, color = TextDim)
            Text("📅 " + Fmt.date(day), fontSize = 16.sp, color = TextMain, fontWeight = FontWeight.Bold)
        }
    }
    if (open) {
        val state = rememberDatePickerState(initialSelectedDateMillis = day * DAY_MS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(Math.floorDiv(it, DAY_MS)) }
                    open = false
                }) { Text("אישור") }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text("ביטול") } },
        ) { DatePicker(state = state, showModeToggle = false) }
    }
}

/** Opens a link in the browser; adds https:// when it was typed without it. */
fun openLink(context: Context, raw: String) {
    val url = raw.trim().let { if (it.startsWith("http://") || it.startsWith("https://")) it else "https://$it" }
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "לא נמצא דפדפן לפתיחת הקישור", Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        Toast.makeText(context, "הקישור לא תקין", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun CrashBanner() {
    val context = LocalContext.current
    var text by remember { mutableStateOf(CrashLog.read(context)) }
    val t = text ?: return
    Column(
        Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(BadSoft)
            .padding(10.dp)
    ) {
        Text("האפליקציה נסגרה בפעם הקודמת בגלל שגיאה", color = TextMain, fontWeight = FontWeight.Bold)
        Row {
            TextButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", t))
            }) { Text("העתקת פרטי השגיאה", color = Green) }
            TextButton(onClick = { CrashLog.clear(context); text = null }) { Text("סגירה", color = TextDim) }
        }
    }
}
