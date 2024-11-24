package com.megahed.eqtarebmenalla.feature_data.data.repository

import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImage
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.QuranListenApi
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.VerseReaders
import com.megahed.eqtarebmenalla.feature_data.domain.repository.QuranListenerRepository
import javax.inject.Inject

class QuranListenerRepositoryImp @Inject constructor(
    private val quranListenApi: QuranListenApi
) : QuranListenerRepository {
    override suspend fun getQuranListener(): QuranListen {
        return quranListenApi.getQuranListener()
    }

    override suspend fun getVerseReaders(): VerseReaders {
        return quranListenApi.getVerseReaders()
    }

    override suspend fun getSoraImages(surah: Int, read: Int): QuranImage {
        return quranListenApi.getSoraImages(surah, read)
    }

    override suspend fun getReadersWithAyatTimings(): List<QuranListenerReader> {
        return quranListenApi.getReadersWithAyatTimings()
    }
}