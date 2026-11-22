package com.hatuka.income.data

import java.time.LocalDate
import java.time.YearMonth

enum class JobKind { EMPLOYEE, SELF }

/** A workplace. [payrollUrl] is the link to its payslip system (only for an employee job). */
data class Job(
    val id: Long,
    val name: String,
    val kind: JobKind,
    val payrollUrl: String = "",
    val color: Int = 0,
)

/**
 * One stretch of work: from [fromDay] to [toDay] (epoch days, inclusive), [days] worked days in it,
 * [amount] ₪ that came in for it. The income belongs to the month of [toDay].
 */
data class WorkEntry(
    val id: Long,
    val jobId: Long,
    val fromDay: Long,
    val toDay: Long,
    val days: Int,
    val amount: Double,
    val note: String = "",
)

/** A one-off income (a gift, a bonus, a single gig…), optionally linked to a job. */
data class OneTime(
    val id: Long,
    val day: Long,
    val amount: Double,
    val title: String,
    val jobId: Long? = null,
)

data class Settings(
    /** Tax credit points: 2.25 for a man, 2.75 for a woman, more for children, degrees etc. */
    val creditPoints: Double = 2.75,
    /** Employee pension deduction, percent of gross. */
    val pensionPct: Double = 6.0,
)

fun Long.toDate(): LocalDate = LocalDate.ofEpochDay(this)
fun Long.toMonth(): YearMonth = YearMonth.from(LocalDate.ofEpochDay(this))
fun today(): Long = LocalDate.now().toEpochDay()
