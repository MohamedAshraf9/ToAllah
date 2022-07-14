package com.megahed.eqtarebmenalla.feature_data.domain.repository

import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen

interface QuranListenerRepository {
    suspend fun getQuranListener(): QuranListen
}