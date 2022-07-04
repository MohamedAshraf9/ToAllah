package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.model.Sora
import kotlinx.coroutines.flow.Flow

interface SoraRepository {

    suspend fun insertSora(sora: Sora)

    suspend fun updateSora(sora: Sora)

    suspend fun deleteSora(sora: Sora)

    suspend fun getSoraById(id:Int): Sora?

    fun getAllSora(): Flow<List<Sora>>
}