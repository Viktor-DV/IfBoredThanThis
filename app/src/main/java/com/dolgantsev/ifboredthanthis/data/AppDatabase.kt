package com.dolgantsev.ifboredthanthis.data

import androidx.room.Database
import androidx.room.RoomDatabase

// База данных Room
@Database(entities = [ActivityEntity::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    // Получить DAO для активностей
    abstract fun activityDao(): ActivityDao
}