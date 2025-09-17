package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(
    tableName = "memorization_sessions",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = DailyTarget::class,
            parentColumns = ["id"],
            childColumns = ["dailyTargetId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [androidx.room.Index("dailyTargetId")]
)
data class MemorizationSession(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dailyTargetId: Long,
    val startTime: Date,
    val endTime: Date? = null,
    val actualDurationMinutes: Int = 0,
    val versesCompleted: Int = 0,
    val sessionType: SessionType = SessionType.LISTENING,
    val notes: String? = null
)

enum class SessionType {
    LISTENING,
    RECITATION,
    REVIEW
}