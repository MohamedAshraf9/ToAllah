package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.megahed.eqtarebmenalla.db.model.UserStreak
import kotlinx.coroutines.flow.Flow
import java.util.Date

@Dao
interface UserStreakDao {

    @Query("SELECT * FROM user_streaks WHERE id = 1 LIMIT 1")
    suspend fun getUserStreak(): UserStreak?

    @Query("SELECT * FROM user_streaks WHERE id = 1 LIMIT 1")
    fun getUserStreakFlow(): Flow<UserStreak?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateStreak(streak: UserStreak)

    @Query("""
        UPDATE user_streaks SET 
        currentStreak = :currentStreak,
        longestStreak = :longestStreak,
        lastCompletionDate = :lastCompletionDate,
        totalDaysCompleted = :totalDaysCompleted,
        totalVersesMemorized = :totalVersesMemorized,
        updatedAt = :updatedAt
        WHERE id = 1
    """)
    suspend fun updateStreak(
        currentStreak: Int,
        longestStreak: Int,
        lastCompletionDate: Date?,
        totalDaysCompleted: Int,
        totalVersesMemorized: Int,
        updatedAt: Date
    )
}