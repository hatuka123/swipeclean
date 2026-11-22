package com.hatuka.income.data

/**
 * Israeli payroll deductions for a monthly salary, tax year 2026 (an estimate, not a payslip).
 *
 * Income tax brackets after the 2026 widening (20% up to 19,000, 31% up to 25,100), credit point 242 ₪.
 * Bituach Leumi + health (employee share): 4.27% up to 7,703 ₪, 12.17% from there up to 51,910 ₪.
 */
object Tax {
    const val YEAR = 2026
    const val CREDIT_POINT = 242.0
    const val BL_REDUCED_UPTO = 7_703.0
    const val BL_MAX = 51_910.0
    const val BL_LOW = 0.0427
    const val BL_HIGH = 0.1217
    /** What a second employer withholds when there is no tax coordination. */
    const val NO_COORDINATION_RATE = 0.47

    /** Upper limit of each monthly bracket and its rate. */
    val brackets: List<Pair<Double, Double>> = listOf(
        7_010.0 to 0.10,
        10_060.0 to 0.14,
        19_000.0 to 0.20,
        25_100.0 to 0.31,
        46_690.0 to 0.35,
        60_130.0 to 0.47,
        Double.POSITIVE_INFINITY to 0.50,
    )

    data class Slice(val from: Double, val to: Double, val rate: Double, val tax: Double)

    data class Result(
        val gross: Double,
        val slices: List<Slice>,
        val taxBeforeCredits: Double,
        val credits: Double,
        val incomeTax: Double,
        val social: Double,
        val pension: Double,
        val marginalRate: Double,
    ) {
        val deductions get() = incomeTax + social + pension
        val net get() = gross - deductions
    }

    fun incomeTaxSlices(gross: Double): List<Slice> {
        val out = mutableListOf<Slice>()
        var lower = 0.0
        for ((upper, rate) in brackets) {
            if (gross <= lower) break
            val top = minOf(gross, upper)
            out += Slice(lower, top, rate, (top - lower) * rate)
            lower = upper
        }
        return out
    }

    fun social(gross: Double): Double {
        val g = gross.coerceIn(0.0, BL_MAX)
        val low = minOf(g, BL_REDUCED_UPTO)
        return low * BL_LOW + (g - low) * BL_HIGH
    }

    fun calc(gross: Double, creditPoints: Double, pensionPct: Double = 0.0): Result {
        val g = maxOf(gross, 0.0)
        val slices = incomeTaxSlices(g)
        val before = slices.sumOf { it.tax }
        val credits = maxOf(creditPoints, 0.0) * CREDIT_POINT
        return Result(
            gross = g,
            slices = slices,
            taxBeforeCredits = before,
            credits = minOf(credits, before),
            incomeTax = maxOf(before - credits, 0.0),
            social = social(g),
            pension = g * maxOf(pensionPct, 0.0) / 100.0,
            marginalRate = slices.lastOrNull()?.rate ?: 0.10,
        )
    }

    /**
     * Several employers in one month. Without coordination the biggest salary gets the credit points and the
     * normal brackets, every other one is taxed at 47% flat. With coordination the sum is taxed as one salary.
     */
    data class MultiJob(val withoutCoordination: Double, val withCoordination: Double) {
        val overpaid get() = withoutCoordination - withCoordination
    }

    fun multiJob(salaries: List<Double>, creditPoints: Double): MultiJob {
        val sorted = salaries.filter { it > 0 }.sortedDescending()
        if (sorted.isEmpty()) return MultiJob(0.0, 0.0)
        val main = calc(sorted.first(), creditPoints).incomeTax
        val rest = sorted.drop(1).sumOf { it * NO_COORDINATION_RATE }
        val together = calc(sorted.sum(), creditPoints).incomeTax
        return MultiJob(main + rest, together)
    }
}
