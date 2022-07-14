package com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen

import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import retrofit2.http.GET
import retrofit2.http.Query

interface QuranListenApi {

    @GET("api/tvs/arabic.json")
    suspend fun getQuranListener(): QuranListen

}