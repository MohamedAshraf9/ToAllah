package com.megahed.eqtarebmenalla.feature_data.data.remote.prayerTime

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IslamicApi {

    @GET("v1/timings")
    suspend fun getIslamicData(
        @Query("latitude") latitude:Double,
        @Query("longitude") longitude:Double,
        @Query("method") method:Int=5,

    ): IslamicInfo
}