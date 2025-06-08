package com.dolgantsev.ifboredthanthis.model

import com.google.gson.annotations.SerializedName

data class ActivityResponse(
    @SerializedName("activity") val activity: String,
    @SerializedName("availability") val availability: Float,
    @SerializedName("type") val type: String,
    @SerializedName("participants") val participants: Int,
    @SerializedName("price") val price: Float,
    @SerializedName("accessibility") val accessibility: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("kidFriendly") val kidFriendly: Boolean,
    @SerializedName("link") val link: String,
    @SerializedName("key") val key: String
)