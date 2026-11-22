package com.hatuka.income

import com.hatuka.income.data.Tax
import org.junit.Assert.assertEquals
import org.junit.Test

class TaxTest {
    private val eps = 0.01

    @Test
    fun belowCreditsPaysNoIncomeTax() {
        // 5,000 × 10% = 500 < 2.75 × 242 = 665.5
        assertEquals(0.0, Tax.calc(5_000.0, 2.75).incomeTax, eps)
    }

    @Test
    fun tenThousand() {
        // 7,010×10% + 2,990×14% = 701 + 418.6 = 1,119.6; minus 665.5 credits
        val r = Tax.calc(10_000.0, 2.75)
        assertEquals(1_119.6, r.taxBeforeCredits, eps)
        assertEquals(454.1, r.incomeTax, eps)
        assertEquals(0.14, r.marginalRate, 0.0)
        // 7,703×4.27% + 2,297×12.17%
        assertEquals(7_703 * 0.0427 + 2_297 * 0.1217, r.social, eps)
    }

    @Test
    fun widenedTwentyPercentBracket() {
        val r = Tax.calc(19_000.0, 0.0)
        assertEquals(0.20, r.marginalRate, 0.0)
        assertEquals(701 + 427.0 + 8_940 * 0.20, r.taxBeforeCredits, eps)
    }

    @Test
    fun socialCappedAtMax() {
        assertEquals(Tax.social(Tax.BL_MAX), Tax.social(80_000.0), eps)
    }

    @Test
    fun secondJobWithoutCoordinationPaysMore() {
        val m = Tax.multiJob(listOf(6_000.0, 3_000.0), 2.75)
        assertEquals(3_000 * 0.47, m.withoutCoordination, eps) // main job is below the credits
        assertEquals(701 + 1_990 * 0.14 - 665.5, m.withCoordination, eps) // 9,000 as one salary
    }
}
