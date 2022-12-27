package com.megahed.eqtarebmenalla.db.dao

import androidx.room.*
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerTimeDao{

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerTime(prayerTime: PrayerTime)

    @Update
    suspend fun updatePrayerTime(prayerTime: PrayerTime)

    @Delete
    suspend fun deletePrayerTime(prayerTime: PrayerTime)


    @Query("SELECT * FROM prayertime WHERE id =:id ")
    fun getPrayerTimeById(id:Int): Flow<PrayerTime?>
}