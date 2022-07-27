package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.customModel.HoursDate
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import kotlinx.coroutines.flow.Flow

interface TasbehRepository {

    suspend fun insertTasbeh(tasbeh: Tasbeh)

    suspend fun updateTasbeh(tasbeh: Tasbeh)

    suspend fun deleteTasbeh(tasbeh: Tasbeh)

    fun getAllTasbeh(): Flow<List<Tasbeh>>

    fun TasbehWithData(): Flow<List<TasbehWithData>>

    fun getTasbehCounter(): Flow<List<TasbehCounter>>

    fun getBestDays():Flow<List<HoursDate>>

    fun getDataOfMonths():Flow<List<HoursDate>>

}