package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.SoraDao
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import kotlinx.coroutines.flow.Flow

class SoraRepositoryImp(
    private val soraDao: SoraDao
): SoraRepository {

    override suspend fun insertSora(sora: Sora) {
       soraDao.insertSora(sora)
    }

    override suspend fun updateSora(sora: Sora) {
       soraDao.updateSora(sora)
    }

    override suspend fun deleteSora(sora: Sora) {
        soraDao.deleteSora(sora)
    }

    override suspend fun getSoraById(id: Int): Sora? {
        return soraDao.getSoraById(id)
    }

    override fun getAllSora(): Flow<List<Sora>> {
        return soraDao.getAllSora()
    }
}