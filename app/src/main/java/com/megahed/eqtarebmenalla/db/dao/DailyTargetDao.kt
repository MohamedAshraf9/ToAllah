package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface DailyTargetDao {

    @Query("SELECT * FROM daily_targets WHERE scheduleId = :scheduleId ORDER BY targetDate ASC")
    fun getTargetsForSchedule(scheduleId: Long): Flow<List<DailyTarget>>

    @Query("SELECT * FROM daily_targets WHERE targetDate = :date AND scheduleId = :scheduleId LIMIT 1")
    suspend fun getTargetForDate(date: Date, scheduleId: Long): DailyTarget?

    @Query("SELECT * FROM daily_targets WHERE targetDate = :date AND scheduleId = :scheduleId LIMIT 1")
    fun getTargetForDateFlow(date: Date, scheduleId: Long): Flow<DailyTarget?>

    @Query("SELECT * FROM daily_targets WHERE id = :id")
    suspend fun getTargetById(id: Long): DailyTarget?

    @Query("SELECT * FROM daily_targets WHERE scheduleId = :scheduleId AND isCompleted = 0 ORDER BY targetDate ASC LIMIT 1")
    suspend fun getNextIncompleteTarget(scheduleId: Long): DailyTarget?

    @Query("SELECT COUNT(*) FROM daily_targets WHERE scheduleId = :scheduleId AND isCompleted = 1")
    suspend fun getCompletedTargetsCount(scheduleId: Long): Int

    @Query("SELECT COUNT(*) FROM daily_targets WHERE scheduleId = :scheduleId")
    suspend fun getTotalTargetsCount(scheduleId: Long): Int

    @Query("""
        SELECT COUNT(*) FROM daily_targets 
        WHERE scheduleId = :scheduleId 
        AND isCompleted = 1 
        AND targetDate >= :startDate 
        AND targetDate <= :endDate
    """)
    suspend fun getCompletedTargetsInRange(scheduleId: Long, startDate: Date, endDate: Date): Int

    @Insert
    suspend fun insertTarget(target: DailyTarget): Long

    @Insert
    suspend fun insertTargets(targets: List<DailyTarget>)

    @Update
    suspend fun updateTarget(target: DailyTarget)

    @Query("UPDATE daily_targets SET isCompleted = 1, completedAt = :completedAt WHERE id = :targetId")
    suspend fun markTargetCompleted(targetId: Long, completedAt: Date)

    @Delete
    suspend fun deleteTarget(target: DailyTarget)

    @Query("DELETE FROM daily_targets WHERE scheduleId = :scheduleId")
    suspend fun deleteTargetsForSchedule(scheduleId: Long)

    @Query("""
    SELECT 
        COALESCE(SUM(endVerse - startVerse + 1), 0) as totalVerses
    FROM daily_targets 
    WHERE scheduleId = :scheduleId
""")
    suspend fun getTotalVersesForSchedule(scheduleId: Long): Int

    @Query("""
    SELECT 
        COALESCE(SUM(endVerse - startVerse + 1), 0) as completedVerses
    FROM daily_targets 
    WHERE scheduleId = :scheduleId AND isCompleted = 1
""")
    suspend fun getCompletedVersesForSchedule(scheduleId: Long): Int
    
    @Query("SELECT * FROM daily_targets WHERE scheduleId = :scheduleId")
    suspend fun getTargetsForScheduleSync(scheduleId: Long): List<DailyTarget>
}