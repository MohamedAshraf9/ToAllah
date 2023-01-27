package com.megahed.eqtarebmenalla.feature_data.data.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.prayerTime.IslamicApi
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import retrofit2.http.Query
import javax.inject.Inject

class IslamicRepositoryImp @Inject constructor(
    private val islamicInfo: IslamicApi
) :IslamicRepository{
    override suspend fun getAzanData(@Query("latitude") latitude:Double,
                                     @Query("longitude") longitude:Double): IslamicInfo {
        return islamicInfo.getIslamicData(latitude=latitude, longitude = longitude)
    }
}