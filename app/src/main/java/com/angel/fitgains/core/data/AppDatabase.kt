package com.angel.fitgains.core.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProfileEntity::class, FoodEntryEntity::class, WeightEntryEntity::class],
    version = 1,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun weightDao(): WeightDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "fitgains.db").build()
    }
}
