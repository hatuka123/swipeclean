package com.hatuka.income.data

import java.time.LocalDate
import java.time.YearMonth

/** An inclusive date range; null in the functions below means "all time". */
data class Period(val from: LocalDate, val to: LocalDate) {
    fun has(day: Long) = day in from.toEpochDay()..to.toEpochDay()

    companion object {
        fun month(m: YearMonth) = Period(m.atDay(1), m.atEndOfMonth())
        fun year(y: Int) = Period(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
    }
}

data class JobTotal(val job: Job, val days: Int, val amount: Double, val count: Int, val oneTime: Double)

object Totals {
    private fun inside(day: Long, p: Period?) = p == null || p.has(day)

    fun perJob(jobs: List<Job>, entries: List<WorkEntry>, oneTimes: List<OneTime>, p: Period?): List<JobTotal> =
        jobs.map { job ->
            val es = entries.filter { it.jobId == job.id && inside(it.toDay, p) }
            JobTotal(
                job = job,
                days = es.sumOf { it.days },
                amount = es.sumOf { it.amount },
                count = es.size,
                oneTime = oneTimes.filter { it.jobId == job.id && inside(it.day, p) }.sumOf { it.amount },
            )
        }

    /** One-off incomes in the period (all of them, linked to a job or not). */
    fun oneTime(oneTimes: List<OneTime>, p: Period?) = oneTimes.filter { inside(it.day, p) }.sumOf { it.amount }

    fun grand(entries: List<WorkEntry>, oneTimes: List<OneTime>, p: Period?) =
        entries.filter { inside(it.toDay, p) }.sumOf { it.amount } + oneTime(oneTimes, p)

    /** Month → total income (work + one-off), newest first. */
    fun byMonth(entries: List<WorkEntry>, oneTimes: List<OneTime>, p: Period?): List<Pair<YearMonth, Double>> {
        val m = sortedMapOf<YearMonth, Double>(compareByDescending { it })
        entries.filter { inside(it.toDay, p) }.forEach { m.merge(it.toDay.toMonth(), it.amount, Double::plus) }
        oneTimes.filter { inside(it.day, p) }.forEach { m.merge(it.day.toMonth(), it.amount, Double::plus) }
        return m.toList()
    }

    /** Salary per employee job in one month (work entries + one-offs linked to that job, e.g. a bonus). */
    fun salaries(jobs: List<Job>, entries: List<WorkEntry>, oneTimes: List<OneTime>, month: YearMonth): Map<Job, Double> {
        val p = Period.month(month)
        return jobs.filter { it.kind == JobKind.EMPLOYEE }
            .associateWith { j ->
                entries.filter { it.jobId == j.id && p.has(it.toDay) }.sumOf { it.amount } +
                    oneTimes.filter { it.jobId == j.id && p.has(it.day) }.sumOf { it.amount }
            }
            .filterValues { it > 0 }
    }

    /** Months of [year] in which money came in from two or more employee jobs. */
    fun multiEmployerMonths(jobs: List<Job>, entries: List<WorkEntry>, oneTimes: List<OneTime>, year: Int): List<YearMonth> =
        (1..12).map { YearMonth.of(year, it) }.filter { salaries(jobs, entries, oneTimes, it).size >= 2 }

    /** Latest month that has any employee salary, or null. */
    fun latestSalaryMonth(jobs: List<Job>, entries: List<WorkEntry>): YearMonth? {
        val ids = jobs.filter { it.kind == JobKind.EMPLOYEE }.map { it.id }.toSet()
        return entries.filter { it.jobId in ids }.maxOfOrNull { it.toDay }?.toMonth()
    }
}
