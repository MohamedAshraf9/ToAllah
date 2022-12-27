package com.megahed.eqtarebmenalla.db.repository

import com.megahed.eqtarebmenalla.db.model.PrayerTime
import kotlinx.coroutines.flow.Flow

interface PrayerTimeRepository {

    suspend fun insertPrayerTime(prayerTime: PrayerTime)
    suspend fun updatePrayerTime(prayerTime: PrayerTime)
    suspend fun deletePrayerTime(prayerTime: PrayerTime)

    fun getPrayerTimeById(id:Int): Flow<PrayerTime?>
}