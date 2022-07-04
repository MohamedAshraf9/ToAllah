package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import kotlinx.coroutines.flow.Flow

interface AyaRepository {

    suspend fun insertAya(aya: Aya)

    suspend fun updateAya(aya: Aya)

    suspend fun deleteAya(aya: Aya)

    suspend fun getAyaById(id:Int): Aya?

     fun getAyaOfSoraId(id:Int): Flow<List<Aya>>
}