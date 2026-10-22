package com.angel.fitgains.core.goals

import kotlin.math.roundToInt

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val factor: Double, val label: String) {
    SEDENTARY(1.2, "Sedentario (poco o nada de ejercicio)"),
    LIGHT(1.375, "Ligero (1-3 días/semana)"),
    MODERATE(1.55, "Moderado (3-5 días/semana)"),
    ACTIVE(1.725, "Activo (6-7 días/semana)"),
    VERY_ACTIVE(1.9, "Muy activo (trabajo físico + entreno)"),
}

enum class Goal(val calorieFactor: Double, val proteinPerKg: Double, val label: String) {
    LOSE_FAT(0.80, 2.2, "Perder grasa"),
    MAINTAIN(1.00, 1.8, "Mantener"),
    GAIN_MUSCLE(1.10, 2.0, "Ganar músculo"),
}

data class BodyData(
    val sex: Sex,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: ActivityLevel,
    val goal: Goal,
)

data class DailyTargets(
    val bmr: Int,
    val tdee: Int,
    val calories: Int,
    val proteinG: Int,
    val carbsG: Int,
    val fatG: Int,
)

object GoalsCalculator {
    private const val FAT_SHARE = 0.27
    private const val MIN_CALORIES_FEMALE = 1200
    private const val MIN_CALORIES_MALE = 1500

    /** Tasa metabólica basal (Mifflin-St Jeor). */
    fun bmr(d: BodyData): Double {
        val base = 10 * d.weightKg + 6.25 * d.heightCm - 5 * d.ageYears
        return if (d.sex == Sex.MALE) base + 5 else base - 161
    }

    fun targets(d: BodyData): DailyTargets {
        val bmr = bmr(d)
        val tdee = bmr * d.activity.factor
        val floor = if (d.sex == Sex.MALE) MIN_CALORIES_MALE else MIN_CALORIES_FEMALE
        val calories = (tdee * d.goal.calorieFactor).coerceAtLeast(floor.toDouble())

        val proteinG = d.weightKg * d.goal.proteinPerKg
        val fatG = calories * FAT_SHARE / 9
        val carbsG = ((calories - proteinG * 4 - fatG * 9) / 4).coerceAtLeast(0.0)

        return DailyTargets(
            bmr = bmr.roundToInt(),
            tdee = tdee.roundToInt(),
            calories = calories.roundToInt(),
            proteinG = proteinG.roundToInt(),
            carbsG = carbsG.roundToInt(),
            fatG = fatG.roundToInt(),
        )
    }
}
