package com.dolgantsev.ifboredthanthis.network

import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Интерфейс для взаимодействия с Bored API
interface ApiService {
    // Получить случайную активность
    @GET("api/activity")
    suspend fun getRandomActivity(): ActivityResponse

    // Получить активность с фильтрами
    @GET("api/activity")
    suspend fun getActivityWithFilters(
        @Query("type") type: String? = null,
        @Query("participants") participants: Int? = null,
        @Query("price") price: Float? = null,
        @Query("minprice") minPrice: Float? = null,
        @Query("maxprice") maxPrice: Float? = null,
        @Query("accessibility") accessibility: Float? = null,
        @Query("minaccessibility") minAccessibility: Float? = null,
        @Query("maxaccessibility") maxAccessibility: Float? = null
    ): ActivityResponse
}