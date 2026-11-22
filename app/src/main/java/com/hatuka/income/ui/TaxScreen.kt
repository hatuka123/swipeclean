package com.hatuka.income.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.hatuka.income.data.JobKind
import com.hatuka.income.data.Period
import com.hatuka.income.data.Store
import com.hatuka.income.data.Tax
import com.hatuka.income.data.Totals
import java.time.YearMonth

@Composable
fun TaxScreen() {
    val now = YearMonth.now()
    val initial = remember {
        val hasNow = Totals.salaries(Store.jobs, Store.entries, Store.oneTimes, now).isNotEmpty()
        (if (hasNow) now else Totals.latestSalaryMonth(Store.jobs, Store.entries) ?: now).toString()
    }
    var monthKey by rememberSaveable { mutableStateOf(initial) }
    val month = YearMonth.parse(monthKey)
    var manual by rememberSaveable { mutableStateOf("") }
    val s = Store.settings

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoordinationAlert()

        // ---------- calculator ----------
        SectionCard("💰 כמה ירד לי מהשכר?") {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { monthKey = month.minusMonths(1).toString() }) { Text("▶", fontSize = 18.sp, color = Green) }
                Text(Fmt.month(month), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                TextButton(onClick = { monthKey = month.plusMonths(1).toString() }) { Text("◀", fontSize = 18.sp, color = Green) }
            }
            val salaries = Totals.salaries(Store.jobs, Store.entries, Store.oneTimes, month)
            val manualGross = Fmt.parse(manual)
            if (salaries.isEmpty() && manualGross == null) {
                Text(
                    "לא רשומה בחודש הזה הכנסה כשכירה. אפשר לעבור חודש עם החצים, או להקליד למטה סכום ברוטו לבדיקה.",
                    color = TextDim, fontSize = 14.sp,
                )
            } else if (manualGross == null) {
                salaries.forEach { (job, v) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dot(jobColor(job.color)); Spacer(Modifier.width(8.dp))
                        Text(job.name, color = TextMain, modifier = Modifier.weight(1f))
                        Text(Fmt.money(v), color = TextMain, fontWeight = FontWeight.Bold)
                    }
                }
                Text("החישוב מניח שהסכום שרשמת הוא ברוטו (לפני ניכויים).", color = TextDim, fontSize = 12.sp)
            }
            NumberField("בדיקה עם ברוטו אחר (לא חובה)", manual, { manual = it }, suffix = "₪ לחודש")

            val list = if (manualGross != null) listOf(manualGross) else salaries.values.toList()
            if (list.isNotEmpty()) {
                val total = list.sum()
                val r = Tax.calc(total, s.creditPoints, s.pensionPct)
                HorizontalDivider(color = Line)
                ResultBlock(r)
                if (list.size >= 2) MultiJobBlock(list)
                BracketTable(r)
            }
        }

        SettingsCard()
        SelfEmployedCard()
        Explanations()
        Text(
            "המספרים לפי חוקי ${Tax.YEAR} והם הערכה בלבד – התלוש האמיתי יכול להיות שונה (זיכויים, הפרשות, הטבות). זה לא ייעוץ מס.",
            color = TextDim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
    }
}

/** Looks at the data and says whether tax coordination is needed. */
@Composable
private fun CoordinationAlert() {
    val year = YearMonth.now().year
    val employeeJobs = Store.jobs.filter { it.kind == JobKind.EMPLOYEE }
    val months = Totals.multiEmployerMonths(Store.jobs, Store.entries, Store.oneTimes, year)
    val (title, body, bg) = when {
        months.isNotEmpty() -> Triple(
            "⚠️ צריך תיאום מס",
            "השנה קיבלת משכורת מיותר ממקום אחד באותו חודש (${months.joinToString(", ") { Fmt.months[it.monthValue - 1] }}). " +
                "בלי תיאום מס, המעסיק ה\"משני\" מנכה ${Fmt.pct(Tax.NO_COORDINATION_RATE)} מס. כדאי לעשות תיאום מס עכשיו – ובינואר שוב לשנה הבאה.",
            BadSoft,
        )
        employeeJobs.size >= 2 -> Triple(
            "⚠️ כנראה צריך תיאום מס",
            "יש לך ${employeeJobs.size} מקומות עבודה כשכירה. אם את עובדת בהם באותה תקופה – צריך תיאום מס, " +
                "אחרת אחד מהם ינכה ${Fmt.pct(Tax.NO_COORDINATION_RATE)}. אם עזבת אחד ועברת לשני – לא חובה, אבל אולי מגיע לך החזר.",
            GoldSoft,
        )
        employeeJobs.size == 1 && Store.jobs.any { it.kind == JobKind.SELF } -> Triple(
            "ℹ️ שכירה + עצמאית",
            "על ההכנסה כעצמאית לא עושים תיאום מס מול המעסיק – היא מדווחת בדוח השנתי. נקודות הזיכוי בדרך כלל נשארות בתלוש.",
            GreenSoft,
        )
        else -> Triple(
            "✅ כרגע לא צריך תיאום מס",
            "יש לך מקום עבודה אחד כשכירה (או אף אחד). אם תתחילי עבודה נוספת כשכירה במקביל – האפליקציה תתריע כאן.",
            GreenSoft,
        )
    }
    SectionCard(null, bg = bg) {
        Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextMain)
        Text(body, color = TextMain, fontSize = 15.sp)
    }
}

@Composable
private fun ResultBlock(r: Tax.Result) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            "את במדרגת מס של ${Fmt.pct(r.marginalRate)}",
            fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Green,
        )
        Text("כלומר מכל שקל נוסף שתרוויחי, ${Fmt.pct(r.marginalRate)} ילכו למס הכנסה.", color = TextDim, fontSize = 13.sp)
        MoneyLine("ברוטו", r.gross, TextMain, bold = true)
        MoneyLine("מס הכנסה", -r.incomeTax, Bad)
        if (r.credits > 0) Text("   (אחרי ${Fmt.num(Store.settings.creditPoints)} נקודות זיכוי ששוות ${Fmt.moneyRound(r.credits)})", color = TextDim, fontSize = 12.sp)
        MoneyLine("ביטוח לאומי + מס בריאות", -r.social, Bad)
        if (r.pension > 0) MoneyLine("פנסיה (${Fmt.num(Store.settings.pensionPct)}%)", -r.pension, Bad)
        HorizontalDivider(color = Line)
        MoneyLine("נטו משוער", r.net, Green, bold = true, big = true)
        Text("סך הכול ירד: ${Fmt.moneyRound(r.deductions)} (${if (r.gross > 0) Fmt.pct((r.deductions / r.gross * 1000).toLong() / 1000.0) else "0%"})", color = TextDim, fontSize = 13.sp)
    }
}

@Composable
private fun MoneyLine(label: String, v: Double, color: Color, bold: Boolean = false, big: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = TextMain, modifier = Modifier.weight(1f), fontSize = if (big) 18.sp else 15.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(
            (if (v < 0) "−" else "") + Fmt.moneyRound(kotlin.math.abs(v)),
            color = color, fontWeight = FontWeight.Bold, fontSize = if (big) 22.sp else 16.sp,
        )
    }
}

@Composable
private fun MultiJobBlock(salaries: List<Double>) {
    val m = Tax.multiJob(salaries, Store.settings.creditPoints)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(GoldSoft).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("שתי עבודות באותו חודש – מס הכנסה:", fontWeight = FontWeight.Bold, color = TextMain)
        Row { Text("בלי תיאום מס", Modifier.weight(1f), color = TextMain); Text(Fmt.moneyRound(m.withoutCoordination), color = Bad, fontWeight = FontWeight.Bold) }
        Row { Text("עם תיאום מס", Modifier.weight(1f), color = TextMain); Text(Fmt.moneyRound(m.withCoordination), color = Green, fontWeight = FontWeight.Bold) }
        if (m.overpaid > 0) {
            Text(
                "בלי תיאום ינוכו לך בערך ${Fmt.moneyRound(m.overpaid)} יותר מדי בחודש. את הכסף אפשר לקבל בחזרה בהחזר מס, אבל רק אחרי סוף השנה.",
                color = TextMain, fontSize = 13.sp,
            )
        }
        Text("(החישוב למעלה הוא עם תיאום מס – כאילו כל המשכורות הן משכורת אחת.)", color = TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun BracketTable(r: Tax.Result) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.animateContentSize()) {
        TextButton(onClick = { open = !open }) { Text(if (open) "▲ הסתרת הפירוט לפי מדרגות" else "▼ איך זה מחושב? פירוט לפי מדרגות", color = Green) }
        if (open) {
            r.slices.forEach { sl ->
                Row {
                    Text(
                        "${Fmt.moneyRound(sl.from)}–${Fmt.moneyRound(sl.to)}",
                        color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f),
                    )
                    Text(Fmt.pct(sl.rate), color = TextDim, fontSize = 13.sp, modifier = Modifier.width(50.dp))
                    Text(Fmt.moneyRound(sl.tax), color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            HorizontalDivider(color = Line)
            Row { Text("מס לפני זיכויים", Modifier.weight(1f), fontSize = 13.sp); Text(Fmt.moneyRound(r.taxBeforeCredits), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Row { Text("פחות נקודות זיכוי", Modifier.weight(1f), fontSize = 13.sp); Text("−" + Fmt.moneyRound(r.credits), fontSize = 13.sp, fontWeight = FontWeight.Bold) }
            Spacer(Modifier.height(6.dp))
            Text(
                "מדרגות ${Tax.YEAR} לחודש: " + Tax.brackets.joinToString(" · ") { (top, rate) ->
                    if (top.isInfinite()) "${Fmt.pct(rate)} מעל" else "${Fmt.pct(rate)} עד ${Fmt.moneyRound(top)}"
                } + ". נקודת זיכוי = ${Fmt.moneyRound(Tax.CREDIT_POINT)} לחודש. ביטוח לאומי ובריאות: " +
                    "${Fmt.pct(Tax.BL_LOW)} עד ${Fmt.moneyRound(Tax.BL_REDUCED_UPTO)}, ${Fmt.pct(Tax.BL_HIGH)} מעל זה.",
                color = TextDim, fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SettingsCard() {
    val s = Store.settings
    var points by remember { mutableStateOf(Fmt.num(s.creditPoints)) }
    var pension by remember { mutableStateOf(Fmt.num(s.pensionPct)) }
    SectionCard("⚙️ הנתונים שלי לחישוב") {
        NumberField("נקודות זיכוי", points, {
            points = it
            Fmt.parse(it)?.let { v -> Store.updateSettings(Store.settings.copy(creditPoints = v)) }
        })
        Text(
            "אישה תושבת ישראל מקבלת 2.75 נקודות. יש תוספות על ילדים, תואר אקדמי, שחרור משירות צבאי/לאומי ועוד – אפשר לראות בתלוש כמה נקודות יש לך.",
            color = TextDim, fontSize = 12.sp,
        )
        NumberField("הפרשה לפנסיה מהשכר", pension, {
            pension = it
            Fmt.parse(it)?.let { v -> Store.updateSettings(Store.settings.copy(pensionPct = v)) }
        }, suffix = "%")
        Text("בדרך כלל 6%. אם אין לך עדיין פנסיה – 0.", color = TextDim, fontSize = 12.sp)
    }
}

@Composable
private fun SelfEmployedCard() {
    val selfJobs = Store.jobs.filter { it.kind == JobKind.SELF }
    if (selfJobs.isEmpty()) return
    val now = YearMonth.now()
    val p = Period(now.atDay(1).withDayOfYear(1), now.atEndOfMonth())
    val perJob = Totals.perJob(Store.jobs, Store.entries, Store.oneTimes, p)
    val selfIncome = perJob.filter { it.job.kind == JobKind.SELF }.sumOf { it.amount + it.oneTime }
    val empIncome = perJob.filter { it.job.kind == JobKind.EMPLOYEE }.sumOf { it.amount + it.oneTime }
    val months = now.monthValue
    val pts = Store.settings.creditPoints
    // Extra income tax the business income adds on top of the salary, spread over the months so far.
    val extra = (Tax.calc((empIncome + selfIncome) / months, pts).incomeTax - Tax.calc(empIncome / months, pts).incomeTax) * months
    SectionCard("🧾 ההכנסה שלך כעצמאית") {
        Text("מתחילת השנה: ${Fmt.money(selfIncome)}", fontWeight = FontWeight.Bold, color = TextMain)
        Text(
            "מהכסף הזה לא ירד מס אוטומטית. בתור עצמאית משלמים מקדמות למס הכנסה ולביטוח לאומי ומגישים דוח שנתי.",
            color = TextMain, fontSize = 14.sp,
        )
        if (selfIncome > 0) {
            Text(
                "הערכה גסה: מס הכנסה על ההכנסה הזאת בערך ${Fmt.moneyRound(extra)} (אם כולה רווח, לפני הוצאות מוכרות ולפני ביטוח לאומי לעצמאים). כדאי לשים את זה בצד.",
                color = TextDim, fontSize = 13.sp,
            )
        }
    }
}

private val faq = listOf(
    "מה זה תיאום מס?" to
        "כל שכירה מקבלת \"הנחה\" במס: נקודות זיכוי ומדרגות מס נמוכות בהתחלה. כשיש שתי משכורות, כל מעסיק לא יודע על השני – " +
        "ולכן אחד מהם (המשני) מנכה מס מקסימלי של 47%. תיאום מס הוא אישור מרשות המסים שאומר לכל מעסיק כמה מס בדיוק לנכות, כך שבסוף משלמים את המס הנכון ולא יותר.",
    "מתי אני חייבת לעשות תיאום מס?" to
        "• כשאת עובדת כשכירה ביותר ממקום אחד באותו זמן (גם אם אחת העבודות קטנה).\n" +
        "• מיד כשמתחילה עבודה נוספת – לא לחכות לסוף השנה.\n" +
        "• בכל שנה מחדש: האישור תקף רק עד 31 בדצמבר, אז בינואר עושים שוב.\n" +
        "• כשהמשכורת באחת העבודות משתנה הרבה – כדאי לעדכן.\n" +
        "• עברת מעבודה לעבודה באמצע השנה (לא במקביל)? לא חובה, אבל יכול להיות שמגיע לך החזר מס.",
    "איך עושים את זה?" to
        "1. באתר רשות המסים יש שירות \"תיאום מס מקוון\" באזור האישי – ממלאים את המשכורות מכל מקום, ומקבלים אישור.\n" +
        "2. אפשר גם בטופס 116 במשרד פקיד השומה.\n" +
        "3. את האישור מעבירים למחלקת השכר בכל מקום עבודה (לפעמים הוא נשלח אליהם ישירות).\n" +
        "4. בטופס 101 שממלאים בכל עבודה מסמנים שיש לך הכנסה נוספת ושביקשת תיאום מס.",
    "מה קורה אם לא עשיתי?" to
        "ינוכה לך יותר מס ממה שצריך. הכסף לא הולך לאיבוד – אפשר לבקש החזר מס (טופס 135) אחרי שהשנה נגמרת, עד 6 שנים אחורה. אבל עד אז הכסף אצל המדינה ולא אצלך.",
    "ואם אני עצמאית?" to
        "על הכנסה מעסק לא עושים תיאום מס מול מעסיק, כי אין תלוש. במקום זה משלמים מקדמות למס הכנסה ולביטוח לאומי (אם נקבעו לך) ומגישים דוח שנתי. " +
        "אם את גם שכירה וגם עצמאית – נקודות הזיכוי בדרך כלל נשארות בתלוש, והדוח השנתי מחבר את הכול ומחשב כמה מס באמת מגיע.",
    "על מה מבוסס החישוב באפליקציה?" to
        "מדרגות מס ההכנסה של ${Tax.YEAR} (כולל הריווח של מדרגות 20% ו־31%), נקודת זיכוי של ${Fmt.moneyRound(Tax.CREDIT_POINT)} לחודש, " +
        "ודמי ביטוח לאומי ובריאות של עובדת שכירה. זה לא לוקח בחשבון קרן השתלמות, הטבות מיוחדות, זיכוי על הפקדה לפנסיה ועוד – אז זו הערכה טובה, לא תלוש.",
)

@Composable
private fun Explanations() {
    SectionCard("❓ הסבר: תיאום מס") {
        faq.forEach { (q, a) -> Faq(q, a) }
    }
}

@Composable
private fun Faq(q: String, a: String) {
    var open by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (open) GreenSoft else CardSoft)
            .border(1.dp, Line, RoundedCornerShape(10.dp))
            .clickable { open = !open }
            .padding(12.dp)
            .animateContentSize(),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(q, fontWeight = FontWeight.Bold, color = TextMain, modifier = Modifier.weight(1f), fontSize = 15.sp)
            Text(if (open) "▲" else "▼", color = Green)
        }
        if (open) {
            Spacer(Modifier.height(6.dp))
            Text(a, color = TextMain, fontSize = 14.sp)
        }
    }
}
