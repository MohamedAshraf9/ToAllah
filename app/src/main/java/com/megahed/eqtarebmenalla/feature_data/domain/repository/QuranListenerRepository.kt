package com.megahed.eqtarebmenalla.feature_data.domain.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.VerseReaders

interface QuranListenerRepository {
    suspend fun getQuranListener(): QuranListen
    suspend fun getVerseReaders(): VerseReaders
}