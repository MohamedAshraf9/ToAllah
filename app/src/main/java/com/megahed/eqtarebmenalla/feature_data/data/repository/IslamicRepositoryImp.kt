package com.megahed.eqtarebmenalla.feature_data.data.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.IslamicApi
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import javax.inject.Inject

class IslamicRepositoryImp @Inject constructor(
    private val islamicInfo: IslamicApi
) :IslamicRepository{
    override suspend fun getAzanData(city: String, country: String): IslamicInfo {
        return islamicInfo.getIslamicData(city,country)
    }
}