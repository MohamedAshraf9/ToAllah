package com.megahed.eqtarebmenalla.feature_data.data.remote.prayerTime

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import retrofit2.http.GET
import retrofit2.http.Query

interface IslamicApi {
    @GET("v1/timingsByCity")
    suspend fun getIslamicData(
        @Query("city") city:String,
        @Query("country") country:String
    ): IslamicInfo
}