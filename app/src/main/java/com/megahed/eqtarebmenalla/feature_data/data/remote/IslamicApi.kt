package com.megahed.eqtarebmenalla.feature_data.data.remote

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.IslamicInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface IslamicApi {
    @GET("v1/timingsByCity")
    suspend fun getIslamicData(
        @Query("city") city:String,
        @Query("country") country:String
    ):IslamicInfo
}