package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "daily_targets",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = MemorizationSchedule::class,
            parentColumns = ["id"],
            childColumns = ["scheduleId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("scheduleId"), androidx.room.Index("targetDate")]
)
data class DailyTarget(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val scheduleId: Long,
    val targetDate: Date,
    val surahId: Int,
    val surahName: String,
    val startVerse: Int,
    val endVerse: Int,
    val estimatedDurationMinutes: Int = 30,
    val isCompleted: Boolean = false,
    val completedAt: Date? = null,
    val completedVerses: Int = 0
)

fun DailyTarget.getTotalVerses(): Int {
    return endVerse - startVerse + 1
}

fun DailyTarget.getCompletionStatusText(): String {
    val total = getTotalVerses()
    return when {
        isCompleted -> "مكتمل"
        completedVerses > 0 -> "مكتمل جزئياً ($completedVerses/$total)"
        else -> "غير مكتمل"
    }
}

fun DailyTarget.getProgressPercentage(): Int {
    val total = getTotalVerses()
    return if (total > 0) (completedVerses * 100) / total else 0
}

fun DailyTarget.getVerseProgressText(): String {
    val total = getTotalVerses()
    return "$completedVerses/$total آيات"
}