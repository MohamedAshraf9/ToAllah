package com.megahed.eqtarebmenalla.feature_data.states

import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.UserStreak
import com.megahed.eqtarebmenalla.feature_data.data.repository.ScheduleProgress
import com.megahed.eqtarebmenalla.feature_data.data.repository.ScheduleVerseProgress

data class MemorizationUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showCelebration: Boolean = false,
    val scheduleProgress: ScheduleProgress? = null,
    val verseProgress: ScheduleVerseProgress? = null,
    val currentSessionId: Long? = null,
    val isSessionActive: Boolean = false,
    val lastCreatedScheduleId: Long? = null
)
fun DailyTarget.getFormattedRange(): String {
    return if (startVerse == endVerse) {
        "Verse $startVerse"
    } else {
        "Verses $startVerse-$endVerse"
    }
}

fun DailyTarget.getTotalVerses(): Int {
    return endVerse - startVerse + 1
}

fun UserStreak.getStreakMessage(): String {
    return when (currentStreak) {
        0 -> "Start your memorization journey today!"
        1 -> "Great start! Keep going!"
        in 2..6 -> "$currentStreak days strong! You're building a habit!"
        7 -> "Amazing! You've completed a full week!"
        in 8..29 -> "$currentStreak days of dedication!"
        30 -> "Incredible! A full month of consistent memorization!"
        else -> "$currentStreak days of unwavering commitment!"
    }
}
