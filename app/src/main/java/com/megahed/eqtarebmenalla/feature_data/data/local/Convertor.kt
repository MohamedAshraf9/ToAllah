package com.megahed.eqtarebmenalla.feature_data.data.local

import androidx.room.ProvidedTypeConverter
import androidx.room.TypeConverter
import com.google.gson.reflect.TypeToken
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.Data
import com.megahed.eqtarebmenalla.feature_data.data.util.JsonParser

@ProvidedTypeConverter
class Converters(
    private val jsonParser: JsonParser
) {
    @TypeConverter
    fun fromMeaningsJson(json: String): List<Data> {
        return jsonParser.fromJson<ArrayList<Data>>(
            json,
            object : TypeToken<ArrayList<Data>>(){}.type
        ) ?: emptyList()
    }

    @TypeConverter
    fun toMeaningsJson(meanings: List<Data>): String {
        return jsonParser.toJson(
            meanings,
            object : TypeToken<ArrayList<Data>>(){}.type
        ) ?: "[]"
    }
}