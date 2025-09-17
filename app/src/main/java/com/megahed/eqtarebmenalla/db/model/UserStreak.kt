package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "user_streaks")
data class UserStreak(
    @PrimaryKey
    val id: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastCompletionDate: Date? = null,
    val totalDaysCompleted: Int = 0,
    val totalVersesMemorized: Int = 0,
    val updatedAt: Date = Date()
)