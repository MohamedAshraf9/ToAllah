package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import kotlinx.coroutines.flow.Flow

interface TasbehRepository {

    suspend fun insertTasbeh(tasbeh: Tasbeh)

    suspend fun updateTasbeh(tasbeh: Tasbeh)

    suspend fun deleteTasbeh(tasbeh: Tasbeh)

    fun getAllTasbeh(): Flow<List<Tasbeh>>

    fun TasbehWithData(): Flow<List<TasbehWithData>>
}