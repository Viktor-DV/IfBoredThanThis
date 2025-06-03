package com.dolgantsev.ifboredthanthis.network

import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("activity")
    suspend fun getRandomActivity(): ActivityResponse

    @GET("activity")
    suspend fun getActivityByType(@Query("type") type: String): ActivityResponse
}