package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.SoraSongDao
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.repository.SoraSongRepository
import kotlinx.coroutines.flow.Flow

class SoraSongRepositoryImp(
    private val soraSongDao: SoraSongDao
):SoraSongRepository {

    override suspend fun insertSoraSong(soraSong: SoraSong) {
        soraSongDao.insertSoraSong(soraSong)
    }

    override suspend fun updateSoraSong(soraSong: SoraSong) {
        soraSongDao.updateSoraSong(soraSong)
    }

    override suspend fun deleteSoraSong(soraSong: SoraSong) {
        soraSongDao.deleteSoraSong(soraSong)
    }

    override suspend fun getSoraSongById(id: Int): SoraSong? {
       return soraSongDao.getSoraSongById(id)
    }

    override fun getFavoriteSoraSong(): Flow<List<SoraSong>> {
        return soraSongDao.getFavoriteSoraSong()
    }
}