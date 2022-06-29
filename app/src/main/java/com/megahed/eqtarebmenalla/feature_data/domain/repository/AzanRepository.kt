package com.megahed.eqtarebmenalla.feature_data.domain.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.AzanInfoDto

interface AzanRepository {

    suspend fun getAzanData(city:String,country:String):List<AzanInfoDto>
}