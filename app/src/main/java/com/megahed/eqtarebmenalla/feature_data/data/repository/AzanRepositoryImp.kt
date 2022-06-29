package com.megahed.eqtarebmenalla.feature_data.data.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.AzanApi
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.AzanInfoDto
import com.megahed.eqtarebmenalla.feature_data.domain.repository.AzanRepository
import javax.inject.Inject

class AzanRepositoryImp @Inject constructor(
    private val azanApi: AzanApi
) :AzanRepository{
    override suspend fun getAzanData(city: String, country: String): List<AzanInfoDto> {
        return azanApi.getAzanData(city,country)
    }
}