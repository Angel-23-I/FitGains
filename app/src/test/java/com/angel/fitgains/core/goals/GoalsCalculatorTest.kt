package com.angel.fitgains.core.goals

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GoalsCalculatorTest {

    private val man = BodyData(Sex.MALE, 30, 180.0, 80.0, ActivityLevel.MODERATE, Goal.MAINTAIN)

    @Test
    fun bmrMale() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, GoalsCalculator.bmr(man), 0.01)
    }

    @Test
    fun bmrFemale() {
        val woman = BodyData(Sex.FEMALE, 25, 165.0, 60.0, ActivityLevel.LIGHT, Goal.MAINTAIN)
        // 600 + 1031.25 - 125 - 161 = 1345.25
        assertEquals(1345.25, GoalsCalculator.bmr(woman), 0.01)
    }

    @Test
    fun maintainEqualsTdee() {
        val t = GoalsCalculator.targets(man)
        assertEquals(2759, t.tdee) // 1780 * 1.55
        assertEquals(t.tdee, t.calories)
    }

    @Test
    fun loseFatIsDeficit() {
        val t = GoalsCalculator.targets(man.copy(goal = Goal.LOSE_FAT))
        assertEquals(2207, t.calories) // 2759 * 0.8
        assertEquals(176, t.proteinG) // 80 * 2.2
    }

    @Test
    fun macrosAddUpToCalories() {
        val t = GoalsCalculator.targets(man.copy(goal = Goal.GAIN_MUSCLE))
        val kcalFromMacros = t.proteinG * 4 + t.carbsG * 4 + t.fatG * 9
        assertTrue(kotlin.math.abs(kcalFromMacros - t.calories) <= 10)
    }

    @Test
    fun caloriesNeverBelowSafeFloor() {
        val small = BodyData(Sex.FEMALE, 70, 150.0, 42.0, ActivityLevel.SEDENTARY, Goal.LOSE_FAT)
        assertEquals(1200, GoalsCalculator.targets(small).calories)
    }
}
