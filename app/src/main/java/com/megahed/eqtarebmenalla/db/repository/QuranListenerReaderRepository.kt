package com.megahed.eqtarebmenalla.db.repository

import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import kotlinx.coroutines.flow.Flow

interface QuranListenerReaderRepository {


    suspend fun insertQuranListenerReader(quranListenerReader: QuranListenerReader)

    suspend fun updateQuranListenerReader(quranListenerReader: QuranListenerReader)

    suspend fun deleteQuranListenerReader(quranListenerReader: QuranListenerReader)

    suspend fun getQuranListenerReaderById(id:String): QuranListenerReader?

    fun getFavoriteQuranListenerReader(): Flow<List<QuranListenerReader>>

    fun getAllQuranListenerReader(): Flow<List<QuranListenerReader>>

    fun getAllFavSorasOfReader():Flow<List<ReaderWithSora>>


}