package com.megahed.eqtarebmenalla.db.repository

import com.megahed.eqtarebmenalla.db.model.PrayerTime

interface PrayerTimeRepository {

    suspend fun insertPrayerTime(prayerTime: PrayerTime)
    suspend fun updatePrayerTime(prayerTime: PrayerTime)
    suspend fun deletePrayerTime(prayerTime: PrayerTime)

    suspend fun getPrayerTimeById(): PrayerTime?
}