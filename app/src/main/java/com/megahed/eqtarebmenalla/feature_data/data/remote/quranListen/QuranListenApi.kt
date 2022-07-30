package com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen

import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.VerseReaders
import retrofit2.http.GET
import retrofit2.http.Query

interface QuranListenApi {

    @GET("api/tvs/arabic.json")
    suspend fun getQuranListener(): QuranListen

    @GET("api/verse/verse_ar.json")
    suspend fun getVerseReaders(): VerseReaders

}