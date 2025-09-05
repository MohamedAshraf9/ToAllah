package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import kotlinx.coroutines.flow.Flow

@Dao
interface MemorizationScheduleDao {

    @Query("SELECT * FROM memorization_schedules WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveSchedules(): Flow<List<MemorizationSchedule>>

    @Query("SELECT * FROM memorization_schedules WHERE id = :id")
    suspend fun getScheduleById(id: Long): MemorizationSchedule?

    @Query("SELECT * FROM memorization_schedules WHERE isActive = 1 LIMIT 1")
    suspend fun getCurrentActiveSchedule(): MemorizationSchedule?

    @Query("SELECT * FROM memorization_schedules WHERE isActive = 1 LIMIT 1")
    fun getCurrentActiveScheduleFlow(): Flow<MemorizationSchedule?>

    @Insert
    suspend fun insertSchedule(schedule: MemorizationSchedule): Long

    @Update
    suspend fun updateSchedule(schedule: MemorizationSchedule)

    @Query("UPDATE memorization_schedules SET isActive = 0 WHERE id != :activeId")
    suspend fun deactivateOtherSchedules(activeId: Long)

    @Delete
    suspend fun deleteSchedule(schedule: MemorizationSchedule)
}