package com.megahed.eqtarebmenalla.feature_data.domain.repository

import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImage
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.VerseReaders
import retrofit2.http.Query

interface QuranListenerRepository {
    suspend fun getQuranListener(): QuranListen
    suspend fun getVerseReaders(): VerseReaders

    suspend fun getSoraImages(
      surah:Int,
      read:Int
    ): QuranImage
}