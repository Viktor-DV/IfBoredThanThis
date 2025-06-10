package com.dolgantsev.ifboredthanthis.model

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import android.util.Log

class ActivityResponseDeserializer : JsonDeserializer<ActivityResponse> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): ActivityResponse {
        val jsonObject = json.asJsonObject
        val activity = jsonObject.get("activity")?.asString ?: ""
        val type = jsonObject.get("type")?.asString ?: ""
        val participants = jsonObject.get("participants")?.asInt
        val price = jsonObject.get("price")?.let { parseFloat(it) } ?: 0f
        val link = jsonObject.get("link")?.asString ?: ""
        val key = jsonObject.get("key")?.asString ?: ""
        val accessibility = jsonObject.get("accessibility")?.let { parseAccessibility(it) } ?: 0f

        return ActivityResponse(
            activity = activity,
            type = type,
            participants = participants,
            price = price,
            link = link,
            key = key,
            accessibility = accessibility
        )
    }

    private fun parseFloat(element: JsonElement): Float {
        return when {
            element.isJsonNull -> 0f
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                try {
                    element.asString.toFloat()
                } catch (e: NumberFormatException) {
                    Log.w("API", "Invalid price format: ${element.asString}, defaulting to 0f")
                    0f
                }
            }
            else -> {
                Log.w("API", "Unknown price format: $element, defaulting to 0f")
                0f
            }
        }
    }

    private fun parseAccessibility(element: JsonElement): Float {
        return when {
            element.isJsonNull -> 0f
            element.isJsonPrimitive && element.asJsonPrimitive.isNumber -> element.asFloat
            element.isJsonPrimitive && element.asJsonPrimitive.isString -> {
                convertAccessibilityString(element.asString)
            }
            else -> {
                Log.w("API", "Unknown accessibility format: $element, defaulting to 0f")
                0f
            }
        }
    }

    private fun convertAccessibilityString(value: String): Float {
        return when (value.lowercase()) {
            "very easy" -> 0.0f
            "easy" -> 0.1f
            "medium" -> 0.3f
            "hard" -> 0.7f
            "very hard" -> 0.9f
            "minor challenges" -> 0.2f
            else -> {
                Log.w("API", "Unknown accessibility string: $value, defaulting to 0f")
                try {
                    value.toFloat()
                } catch (e: NumberFormatException) {
                    0f
                }
            }
        }
    }
}