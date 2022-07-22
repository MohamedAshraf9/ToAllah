package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.SoraSong
import kotlinx.coroutines.flow.Flow

interface SoraSongRepository {


    suspend fun insertSoraSong(soraSong: SoraSong)

    suspend fun updateSoraSong(soraSong: SoraSong)

    suspend fun deleteSoraSong(soraSong: SoraSong)

    suspend fun getSoraSongById(id:Int): SoraSong?

    fun getFavoriteSoraSong(): Flow<List<SoraSong>>

}