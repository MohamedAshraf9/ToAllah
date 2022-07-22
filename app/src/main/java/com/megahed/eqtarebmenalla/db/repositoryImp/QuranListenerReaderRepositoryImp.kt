package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.QuranListenerReaderDao
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import kotlinx.coroutines.flow.Flow

class QuranListenerReaderRepositoryImp (
        private val quranListenerReaderDao: QuranListenerReaderDao
): QuranListenerReaderRepository {

        override suspend fun insertQuranListenerReader(quranListenerReader: QuranListenerReader) {
                quranListenerReaderDao.insertQuranListenerReader(quranListenerReader)
        }

        override suspend fun updateQuranListenerReader(quranListenerReader: QuranListenerReader) {
                quranListenerReaderDao.updateQuranListenerReader(quranListenerReader)
        }

        override suspend fun deleteQuranListenerReader(quranListenerReader: QuranListenerReader) {
                quranListenerReaderDao.deleteQuranListenerReader(quranListenerReader)
        }

        override suspend fun getQuranListenerReaderById(id: String): QuranListenerReader? {
               return quranListenerReaderDao.getQuranListenerReaderById(id)
        }

        override fun getFavoriteQuranListenerReader(): Flow<List<QuranListenerReader>> {
               return quranListenerReaderDao.getFavoriteQuranListenerReader()
        }

        override fun getAllQuranListenerReader(): Flow<List<QuranListenerReader>> {
                return quranListenerReaderDao.getAllQuranListenerReader()
        }
}