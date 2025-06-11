package com.dolgantsev.ifboredthanthis.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// DAO для операций с базой данных активностей
@Dao
interface ActivityDao {
    // Добавить активность
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: ActivityEntity)

    // Получить все активности истории (сортировка по времени)
    @Query("SELECT * FROM activities WHERE isFavorite = 0 ORDER BY timestamp DESC")
    fun getHistoryActivities(): Flow<List<ActivityEntity>>

    // Получить истории по типу
    @Query("SELECT * FROM activities WHERE isFavorite = 0 AND type = :type ORDER BY timestamp DESC")
    fun getHistoryActivitiesByType(type: String): Flow<List<ActivityEntity>>

    // Получить все избранные активности
    @Query("SELECT * FROM activities WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteActivities(): Flow<List<ActivityEntity>>

    // Получить избранное по типу
    @Query("SELECT * FROM activities WHERE isFavorite = 1 AND type = :type ORDER BY timestamp DESC")
    fun getFavoriteActivitiesByType(type: String): Flow<List<ActivityEntity>>

    // Удалить самые старые активности истории (сверх 50)
    @Query("DELETE FROM activities WHERE isFavorite = 0 AND id IN (SELECT id FROM activities WHERE isFavorite = 0 ORDER BY timestamp ASC LIMIT :count)")
    suspend fun deleteOldestHistory(count: Int)

    // Получить количество записей в истории
    @Query("SELECT COUNT(*) FROM activities WHERE isFavorite = 0")
    suspend fun getHistoryCount(): Int

    // Получить количество избранных записей
    @Query("SELECT COUNT(*) FROM activities WHERE isFavorite = 1")
    suspend fun getFavoritesCount(): Int

    // Обновить статус избранного
    @Query("UPDATE activities SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavoriteStatus(id: Long, isFavorite: Boolean)

    // Найти активность по содержимому
    @Query("SELECT * FROM activities WHERE activity = :activity AND type = :type AND participants = :participants AND price = :price AND link = :link LIMIT 1")
    suspend fun findActivity(activity: String, type: String, participants: Int, price: Float, link: String): ActivityEntity?

    // Удалить активности по ID
    @Query("DELETE FROM activities WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    // Удалить все записи истории
    @Query("DELETE FROM activities WHERE isFavorite = 0")
    suspend fun deleteAllHistory()

    // Удалить все избранные записи
    @Query("DELETE FROM activities WHERE isFavorite = 1")
    suspend fun deleteAllFavorites()
}