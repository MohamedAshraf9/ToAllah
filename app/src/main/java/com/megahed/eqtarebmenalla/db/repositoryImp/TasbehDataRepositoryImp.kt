package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.SoraDao
import com.megahed.eqtarebmenalla.db.dao.TasbehDataDao
import com.megahed.eqtarebmenalla.db.model.TasbehData
import com.megahed.eqtarebmenalla.db.repository.TasbehDataRepository
import kotlinx.coroutines.flow.Flow

class TasbehDataRepositoryImp(
private val tasbehDataDao: TasbehDataDao
): TasbehDataRepository{

    override suspend fun insertTasbehData(tasbehData: TasbehData) {
        tasbehDataDao.insertTasbehData(tasbehData)
    }

    override suspend fun updateTasbehData(tasbehData: TasbehData) {
        tasbehDataDao.updateTasbehData(tasbehData)
    }

    override suspend fun deleteTasbehData(tasbehData: TasbehData) {
        tasbehDataDao.deleteTasbehData(tasbehData)
    }

    override fun getAllTasbehData(): Flow<List<TasbehData>> {
        return tasbehDataDao.getAllTasbehData()
    }
}