package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.customModel.HoursDate
import com.megahed.eqtarebmenalla.db.customModel.TasbehCounter
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehWithData
import kotlinx.coroutines.flow.Flow

@Dao
interface TasbehDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasbeh(tasbeh: Tasbeh)

    @Update
    suspend fun updateTasbeh(tasbeh: Tasbeh)

    @Delete
    suspend fun deleteTasbeh(tasbeh: Tasbeh)


    @Query("SELECT * FROM tasbeh")
    fun getAllTasbeh(): Flow<List<Tasbeh>>


    @Transaction
    @Query("SELECT * FROM tasbeh")
    fun getTasbehWithData(): Flow<List<TasbehWithData>>


    @Query("SELECT t.tasbehName as tasbehName,sum(td.target) as count  FROM tasbeh t ,tasbehdata td where t.id=td.tasbehId group by t.id")
    fun getTasbehCounter(): Flow<List<TasbehCounter>>

    @Query("SELECT sum(td.target) as count , td.time as date FROM tasbehdata td,tasbeh t where t.id=td.tasbehId group by date(td.time/1000, 'unixepoch') ")
    fun getBestDays():Flow<List<HoursDate>>

    @Query("SELECT sum(td.target) as count,td.time as date FROM tasbehdata td,tasbeh t   where t.id=td.tasbehId group by strftime('%Y-%m', td.time/1000,'unixepoch','localtime')")
    fun getDataOfMonths():Flow<List<HoursDate>>



}