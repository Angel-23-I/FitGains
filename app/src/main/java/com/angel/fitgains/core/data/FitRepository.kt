package com.angel.fitgains.core.data

import androidx.room.withTransaction
import com.angel.fitgains.core.goals.BodyData
import com.angel.fitgains.core.goals.DailyTargets
import com.angel.fitgains.core.goals.GoalsCalculator
import com.angel.fitgains.core.nutrition.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class FitRepository(private val db: AppDatabase) {
    private val profileDao = db.profileDao()
    private val foodDao = db.foodEntryDao()
    private val weightDao = db.weightDao()

    val profile: Flow<ProfileEntity?> = profileDao.observe()

    val targets: Flow<DailyTargets?> =
        profile.map { p -> p?.let { GoalsCalculator.targets(it.toBodyData()) } }

    val weights: Flow<List<WeightEntryEntity>> = weightDao.observeAll()

    suspend fun hasProfile() = profileDao.get() != null

    suspend fun saveProfile(data: BodyData) = db.withTransaction {
        profileDao.save(ProfileEntity.from(data))
        weightDao.upsert(WeightEntryEntity(date = today(), weightKg = data.weightKg))
    }

    /** Guarda un peso y recalcula las metas, porque dependen del peso actual. */
    suspend fun logWeight(weightKg: Double) = db.withTransaction {
        weightDao.upsert(WeightEntryEntity(date = today(), weightKg = weightKg))
        profileDao.get()?.let { profileDao.save(it.copy(weightKg = weightKg)) }
    }

    fun entries(date: String): Flow<List<FoodEntryEntity>> = foodDao.observeDay(date)

    fun totals(date: String): Flow<DayTotals> = foodDao.observeTotals(date)

    fun dailyKcal(days: Long): Flow<List<DailyKcal>> =
        foodDao.observeDailyKcal(LocalDate.now().minusDays(days - 1).toString())

    suspend fun addFoods(meal: Meal, items: List<FoodItem>, source: String, date: String = today()) {
        foodDao.insertAll(items.map { item ->
            val n = item.totals()
            FoodEntryEntity(
                date = date,
                meal = meal.name,
                name = item.name,
                grams = item.grams,
                kcal = n.kcal,
                proteinG = n.proteinG,
                carbsG = n.carbsG,
                fatG = n.fatG,
                fiberG = n.fiberG,
                source = source,
            )
        })
    }

    suspend fun deleteEntry(id: Long) = foodDao.delete(id)

    companion object {
        fun today(): String = LocalDate.now().toString()
    }
}
