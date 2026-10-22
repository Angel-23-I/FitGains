package com.angel.fitgains.core.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.angel.fitgains.core.goals.ActivityLevel
import com.angel.fitgains.core.goals.BodyData
import com.angel.fitgains.core.goals.Goal
import com.angel.fitgains.core.goals.Sex

enum class Meal(val label: String) {
    BREAKFAST("Desayuno"),
    LUNCH("Almuerzo"),
    DINNER("Cena"),
    SNACK("Snacks"),
}

/** Solo hay un perfil por dispositivo (id fijo = 1). */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val sex: String,
    val ageYears: Int,
    val heightCm: Double,
    val weightKg: Double,
    val activity: String,
    val goal: String,
) {
    fun toBodyData() = BodyData(
        sex = Sex.valueOf(sex),
        ageYears = ageYears,
        heightCm = heightCm,
        weightKg = weightKg,
        activity = ActivityLevel.valueOf(activity),
        goal = Goal.valueOf(goal),
    )

    companion object {
        fun from(d: BodyData) = ProfileEntity(
            sex = d.sex.name,
            ageYears = d.ageYears,
            heightCm = d.heightCm,
            weightKg = d.weightKg,
            activity = d.activity.name,
            goal = d.goal.name,
        )
    }
}

/** Valores ya calculados para los gramos consumidos (no por 100 g). */
@Entity(tableName = "food_entry", indices = [Index("date")])
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // yyyy-MM-dd
    val meal: String,
    val name: String,
    val grams: Double,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double,
    val source: String, // "photo" | "search" | "barcode"
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "weight_entry", indices = [Index(value = ["date"], unique = true)])
data class WeightEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val weightKg: Double,
)

data class DayTotals(
    val kcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
)

data class DailyKcal(val date: String, val kcal: Double)
