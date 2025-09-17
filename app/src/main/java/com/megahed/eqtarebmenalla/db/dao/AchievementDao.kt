package com.megahed.eqtarebmenalla.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.megahed.eqtarebmenalla.db.model.Achievement
import com.megahed.eqtarebmenalla.db.model.AchievementType
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievements ORDER BY unlockedAt DESC")
    fun getAllAchievements(): Flow<List<Achievement>>

    @Query("SELECT * FROM achievements WHERE type = :type")
    suspend fun getAchievementsByType(type: AchievementType): List<Achievement>

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getAchievementCount(): Int

    @Insert
    suspend fun insertAchievement(achievement: Achievement): Long

    @Insert
    suspend fun insertAchievements(achievements: List<Achievement>)

    @Delete
    suspend fun deleteAchievement(achievement: Achievement)
}