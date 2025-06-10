package com.dolgantsev.ifboredthanthis.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Сущность для хранения активностей в базе данных
@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val activity: String,
    val type: String,
    val participants: Int,
    val price: Float,
    val link: String,
    val timestamp: Long,
    val isFavorite: Boolean
)