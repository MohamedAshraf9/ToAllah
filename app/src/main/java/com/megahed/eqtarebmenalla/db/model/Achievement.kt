package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "achievements")
data class Achievement(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: AchievementType,
    val title: String,
    val description: String,
    val iconResource: String? = null,
    val unlockedAt: Date,
    val value: Int = 0
)

enum class AchievementType {
    FIRST_DAY,
    WEEK_STREAK,
    MONTH_STREAK,
    SURAH_COMPLETED,
    HUNDRED_VERSES,
    CONSISTENT_WEEK
}