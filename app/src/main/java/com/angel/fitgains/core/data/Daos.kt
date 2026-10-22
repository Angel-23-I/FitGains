package com.angel.fitgains.core.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun save(profile: ProfileEntity)
}

@Dao
interface FoodEntryDao {
    @Query("SELECT * FROM food_entry WHERE date = :date ORDER BY createdAt")
    fun observeDay(date: String): Flow<List<FoodEntryEntity>>

    @Query(
        "SELECT COALESCE(SUM(kcal),0) AS kcal, COALESCE(SUM(proteinG),0) AS proteinG, " +
            "COALESCE(SUM(carbsG),0) AS carbsG, COALESCE(SUM(fatG),0) AS fatG " +
            "FROM food_entry WHERE date = :date"
    )
    fun observeTotals(date: String): Flow<DayTotals>

    @Query(
        "SELECT date, SUM(kcal) AS kcal FROM food_entry WHERE date >= :fromDate " +
            "GROUP BY date ORDER BY date"
    )
    fun observeDailyKcal(fromDate: String): Flow<List<DailyKcal>>

    @Insert
    suspend fun insertAll(entries: List<FoodEntryEntity>)

    @Query("DELETE FROM food_entry WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entry ORDER BY date")
    fun observeAll(): Flow<List<WeightEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: WeightEntryEntity)
}
