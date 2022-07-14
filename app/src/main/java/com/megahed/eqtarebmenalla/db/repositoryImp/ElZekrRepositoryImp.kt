package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.ElZekrDao
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.repository.ElZekrRepository
import kotlinx.coroutines.flow.Flow

class ElZekrRepositoryImp (
    private val elZekrDao: ElZekrDao
):ElZekrRepository {

    override suspend fun insertElZekr(elZekr: ElZekr) {
        elZekrDao.insertElZekr(elZekr)
    }

    override suspend fun updateElZekr(elZekr: ElZekr) {
        elZekrDao.updateElZekr(elZekr)
    }

    override suspend fun deleteElZekr(elZekr: ElZekr) {
        elZekrDao.deleteElZekr(elZekr)
    }

    override fun getElZekrOfCatId(id: Int): Flow<List<ElZekr>> {
        return elZekrDao.getElZekrOfCatId(id)
    }
}