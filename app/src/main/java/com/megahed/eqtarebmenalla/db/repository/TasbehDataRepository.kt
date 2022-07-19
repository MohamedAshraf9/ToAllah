package com.megahed.eqtarebmenalla.db.repository

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.TasbehData
import kotlinx.coroutines.flow.Flow
import java.util.*

interface TasbehDataRepository {

    suspend fun insertTasbehData(tasbehData: TasbehData)

    suspend fun updateTasbehData(tasbehData: TasbehData)

    suspend fun deleteTasbehData(tasbehData: TasbehData)

    fun getAllTasbehData(): Flow<List<TasbehData>>

    suspend fun getTasbehDataToday(id:Int,str:Date,end:Date): TasbehData?

}