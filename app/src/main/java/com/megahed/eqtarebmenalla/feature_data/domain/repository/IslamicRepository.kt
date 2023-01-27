package com.megahed.eqtarebmenalla.feature_data.domain.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import retrofit2.http.Query

interface IslamicRepository {

    suspend fun getAzanData(@Query("latitude") latitude:Double,
                            @Query("longitude") longitude:Double): IslamicInfo
}