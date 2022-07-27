package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.customModel.HoursDate
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter
import com.megahed.eqtarebmenalla.db.dao.TasbehDao
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import com.megahed.eqtarebmenalla.db.repository.TasbehRepository
import kotlinx.coroutines.flow.Flow


class TasbehRepositoryImp (
    private val tasbehDao: TasbehDao
): TasbehRepository {
    override suspend fun insertTasbeh(tasbeh: Tasbeh) {
        tasbehDao.insertTasbeh(tasbeh)
    }

    override suspend fun updateTasbeh(tasbeh: Tasbeh) {
        tasbehDao.updateTasbeh(tasbeh)
    }

    override suspend fun deleteTasbeh(tasbeh: Tasbeh) {
        tasbehDao.deleteTasbeh(tasbeh)
    }

    override fun getAllTasbeh(): Flow<List<Tasbeh>> {
      return tasbehDao.getAllTasbeh()
    }

    override fun TasbehWithData(): Flow<List<TasbehWithData>> {
        return tasbehDao.getTasbehWithData()
    }

    override fun getTasbehCounter(): Flow<List<TasbehCounter>> {
        return tasbehDao.getTasbehCounter()
    }

    override fun getBestDays(): Flow<List<HoursDate>> {
        return tasbehDao.getBestDays()
    }

    override fun getDataOfMonths(): Flow<List<HoursDate>> {
        return tasbehDao.getDataOfMonths()
    }
}