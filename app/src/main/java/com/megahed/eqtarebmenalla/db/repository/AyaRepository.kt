package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.PrayerTime

interface AyaRepository {

    suspend fun insertAya(aya: Aya)

    suspend fun updateAya(aya: Aya)

    suspend fun deleteAya(aya: Aya)

    suspend fun getAyaById(id:Int): Aya?

    suspend fun getAyaOfSoraId(id:Int): Aya?
}