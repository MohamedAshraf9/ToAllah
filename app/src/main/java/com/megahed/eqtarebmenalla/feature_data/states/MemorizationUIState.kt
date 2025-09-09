package com.megahed.eqtarebmenalla.feature_data.states

import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.UserStreak
import com.megahed.eqtarebmenalla.feature_data.data.repository.ScheduleProgress

data class MemorizationUiState(
    val isLoading: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val showCelebration: Boolean = false,
    val isSessionActive: Boolean = false,
    val currentSessionId: Long? = null,
    val lastCreatedScheduleId: Long? = null,
    val scheduleProgress: ScheduleProgress? = null,
    val verseProgress: VerseProgress? = null
)

data class ScheduleProgress(
    val completedTargets: Int,
    val totalTargets: Int,
    val progressPercentage: Int
)

data class VerseProgress(
    val completedVerses: Int,
    val totalVerses: Int,
    val progressPercentage: Int
)

enum class SessionType {
    READING, LISTENING, WRITING, REVIEWING
}
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
        0 -> "أبدأ رحلتك في حفظ القرآن الكريم اليوم!"
        1 -> "بداية رائعة أستمر!"
        in 2..6 -> "$currentStreak تتحرك بقوة هذه الأيام وتقوم ببناء عادة جميلة في حفظ القرآن!"
        7 -> "هذا رائع لقد أكملت أسبوعاً كاملاً من حفظ القرآن الكريم"
        in 8..29 -> "$currentStreak يوم من العمل الجاد!"
        30 -> "هذا مذهل لقد أكملت شهراً في حفظ القرآن الكريم"
        else -> "$currentStreak يوم من الإلتزام التام هذا رائع"
    }
}
