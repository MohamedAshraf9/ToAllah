package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.megahed.eqtarebmenalla.db.model.MemorizationSession
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface MemorizationSessionDao {

    @Query("SELECT * FROM memorization_sessions WHERE dailyTargetId = :targetId ORDER BY startTime DESC")
    fun getSessionsForTarget(targetId: Long): Flow<List<MemorizationSession>>

    @Query("SELECT * FROM memorization_sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): MemorizationSession?

    @Query("""
        SELECT SUM(actualDurationMinutes) FROM memorization_sessions 
        WHERE startTime >= :startDate AND startTime <= :endDate
    """)
    suspend fun getTotalStudyTimeInRange(startDate: Date, endDate: Date): Int?

    @Query("SELECT * FROM memorization_sessions ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int = 10): List<MemorizationSession>

    @Insert
    suspend fun insertSession(session: MemorizationSession): Long

    @Update
    suspend fun updateSession(session: MemorizationSession)

    @Query("UPDATE memorization_sessions SET endTime = :endTime, actualDurationMinutes = :duration WHERE id = :sessionId")
    suspend fun completeSession(sessionId: Long, endTime: Date, duration: Int)

    @Delete
    suspend fun deleteSession(session: MemorizationSession)
}
