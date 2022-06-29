package com.megahed.eqtarebmenalla.feature_data.data.remote

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.AzanInfoDto
import retrofit2.http.GET
import retrofit2.http.Path

interface AzanApi {
    @GET("/v1/timingsByCity?city={city}&country={country}&method=8")
    suspend fun getAzanData(
        @Path("city") city:String,
        @Path("country") country:String
    ):List<AzanInfoDto>
}