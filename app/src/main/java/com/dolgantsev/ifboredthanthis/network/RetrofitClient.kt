package com.dolgantsev.ifboredthanthis.network

import com.dolgantsev.ifboredthanthis.model.ActivityResponse
import com.dolgantsev.ifboredthanthis.model.ActivityResponseDeserializer
import com.google.gson.GsonBuilder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    private const val BASE_URL = "https://bored-api.appbrewery.com/"

    val apiService: ApiService by lazy {
        val gson = GsonBuilder()
            .registerTypeAdapter(ActivityResponse::class.java, ActivityResponseDeserializer())
            .create()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }
}