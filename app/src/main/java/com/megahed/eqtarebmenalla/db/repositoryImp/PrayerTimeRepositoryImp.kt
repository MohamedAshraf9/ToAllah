package com.megahed.eqtarebmenalla.db.repositoryImp

import com.megahed.eqtarebmenalla.db.dao.PrayerTimeDao
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository

class PrayerTimeRepositoryImp(
    private val prayerTimeDao: PrayerTimeDao
): PrayerTimeRepository {

    override suspend fun insertPrayerTime(prayerTime: PrayerTime) {
        prayerTimeDao.insertPrayerTime(prayerTime)
    }

    override suspend fun updatePrayerTime(prayerTime: PrayerTime) {
        prayerTimeDao.updatePrayerTime(prayerTime)
    }

    override suspend fun deletePrayerTime(prayerTime: PrayerTime) {
        prayerTimeDao.deletePrayerTime(prayerTime)
    }

    override suspend fun getPrayerTimeById(id:Int): PrayerTime? {
        return prayerTimeDao.getPrayerTimeById(id)
    }
}