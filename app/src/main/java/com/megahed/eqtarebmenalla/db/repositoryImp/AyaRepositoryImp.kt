package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.AyaDao
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.repository.AyaRepository
import kotlinx.coroutines.flow.Flow

class AyaRepositoryImp(
    private val ayaDao: AyaDao
): AyaRepository {

    override suspend fun insertAya(aya: Aya) {
        ayaDao.insertAya(aya)
    }

    override suspend fun updateAya(aya: Aya) {
        ayaDao.updateAya(aya)
    }

    override suspend fun deleteAya(aya: Aya) {
        ayaDao.deleteAya(aya)
    }

    override suspend fun getAyaById(id: Int): Aya? {
       return ayaDao.getAyaById(id)
    }

    override fun getAyaOfSoraId(id: Int): Flow<List<Aya>> {
        return ayaDao.getAyaOfSoraId(id)
    }


    override fun getFavoriteAya(): Flow<List<Aya>> {
        return ayaDao.getFavoriteAya()
    }
}