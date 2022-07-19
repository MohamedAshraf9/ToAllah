package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.TasbehData
import kotlinx.coroutines.flow.Flow
import java.util.*

@Dao
interface TasbehDataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbehData(tasbehData: TasbehData)

    @Update
    suspend fun updateTasbehData(tasbehData: TasbehData)

    @Delete
    suspend fun deleteTasbehData(tasbehData: TasbehData)


    @Query("SELECT * FROM tasbehdata")
    fun getAllTasbehData(): Flow<List<TasbehData>>

    @Query("SELECT * FROM tasbehdata WHERE time between :str and :end ")
    suspend fun getTasbehDataToday(str:Date,end:Date): TasbehData?
}