package com.megahed.eqtarebmenalla.feature_data.data.repository

import com.megahed.eqtarebmenalla.db.dao.*
import com.megahed.eqtarebmenalla.db.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemorizationRepository @Inject constructor(
    private val scheduleDao: MemorizationScheduleDao,
    private val dailyTargetDao: DailyTargetDao,
    private val sessionDao: MemorizationSessionDao,
    private val streakDao: UserStreakDao,
    private val achievementDao: AchievementDao,
) {
    fun getActiveSchedules(): Flow<List<MemorizationSchedule>> = scheduleDao.getActiveSchedules()

    fun getCurrentActiveSchedule(): Flow<MemorizationSchedule?> =
        scheduleDao.getCurrentActiveScheduleFlow()

    suspend fun createSchedule(
        title: String,
        description: String?,
        startDate: Date,
        endDate: Date,
        dailyTargets: List<DailyTarget>,
    ): Long {
        val schedule = MemorizationSchedule(
            title = title,
            description = description,
            startDate = startDate,
            endDate = endDate,
            isActive = true,
            createdAt = Date(),
            updatedAt = Date()
        )

        val scheduleId = scheduleDao.insertSchedule(schedule)
        scheduleDao.deactivateOtherSchedules(scheduleId)
        val targetsWithScheduleId = dailyTargets.map { it.copy(scheduleId = scheduleId) }
        dailyTargetDao.insertTargets(targetsWithScheduleId)
        val currentStreak = streakDao.getUserStreak()
        if (currentStreak == null) {
            streakDao.insertOrUpdateStreak(UserStreak())
        }

        return scheduleId
    }

    suspend fun updateSchedule(schedule: MemorizationSchedule) {
        scheduleDao.updateSchedule(schedule.copy(updatedAt = Date()))
    }

    suspend fun deleteSchedule(scheduleId: Long) {
        val schedule = scheduleDao.getScheduleById(scheduleId)
        schedule?.let {
            dailyTargetDao.deleteTargetsForSchedule(scheduleId)
            scheduleDao.deleteSchedule(it)
        }
    }
    suspend fun updateVerseProgress(targetId: Long, completedVerses: Int) {
        dailyTargetDao.updateVerseProgress(targetId, completedVerses, Date())

        val target = dailyTargetDao.getTargetById(targetId)
        if (target != null && target.isCompleted) {
            updateStreak()
            checkAndUnlockAchievements()
        }
    }

    suspend fun markTargetCompleted(targetId: Long) {
        val target = dailyTargetDao.getTargetById(targetId)
        target?.let {
            val totalVerses = it.endVerse - it.startVerse + 1
            dailyTargetDao.updateVerseProgress(targetId, totalVerses, Date())
            updateStreak()
            checkAndUnlockAchievements()
        }
    }

    suspend fun getTodayTargetProgress(): DailyTargetProgress? {
        val currentSchedule = scheduleDao.getCurrentActiveSchedule()
        return currentSchedule?.let { schedule ->
            val today = normalizeDate()
            val target = dailyTargetDao.getTargetForDate(today, schedule.id)
            target?.let {
                DailyTargetProgress(
                    target = it,
                    totalVerses = it.getTotalVerses(),
                    completedVerses = it.completedVerses,
                    progressPercentage = it.getProgressPercentage(),
                    isCompleted = it.isCompleted
                )
            }
        }
    }

    fun getTargetsForSchedule(scheduleId: Long): Flow<List<DailyTarget>> =
        dailyTargetDao.getTargetsForSchedule(scheduleId)

    private fun normalizeDate(date: Date = Date()): Date {
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time
    }

    suspend fun getTodayTarget(): DailyTarget? {
        val currentSchedule = scheduleDao.getCurrentActiveSchedule()
        return currentSchedule?.let { schedule ->
            val today = normalizeDate()
            dailyTargetDao.getTargetForDate(today, schedule.id)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun getTodayTargetFlow(): Flow<DailyTarget?> =
        scheduleDao.getCurrentActiveScheduleFlow().flatMapLatest { schedule ->
            if (schedule == null) {
                flowOf(null)
            } else {
                val today = normalizeDate()
                dailyTargetDao.getTargetForDateFlow(today, schedule.id)
            }
        }


    suspend fun getCompletedTargetsInRange(scheduleId: Long, startDate: Date, endDate: Date): Int {
        return dailyTargetDao.getCompletedTargetsInRange(scheduleId, startDate, endDate)
    }

    suspend fun startMemorizationSession(
        dailyTargetId: Long,
        sessionType: SessionType,
    ): Long {
        val session = MemorizationSession(
            dailyTargetId = dailyTargetId,
            startTime = Date(),
            sessionType = sessionType
        )
        return sessionDao.insertSession(session)
    }

    suspend fun completeSession(
        sessionId: Long,
        versesCompleted: Int,
        notes: String?,
    ) {
        val session = sessionDao.getSessionById(sessionId)
        session?.let {
            val endTime = Date()
            val duration =
                ((endTime.time - it.startTime.time) / 60000).toInt()

            sessionDao.updateSession(
                it.copy(
                    endTime = endTime,
                    actualDurationMinutes = duration,
                    versesCompleted = versesCompleted,
                    notes = notes
                )
            )
        }
    }

    fun getSessionsForTarget(targetId: Long): Flow<List<MemorizationSession>> =
        sessionDao.getSessionsForTarget(targetId)

    suspend fun getTotalStudyTimeThisWeek(): Int {
        val calendar = Calendar.getInstance()
        val endDate = calendar.time
        calendar.add(Calendar.DAY_OF_WEEK, -7)
        val startDate = calendar.time

        return sessionDao.getTotalStudyTimeInRange(startDate, endDate) ?: 0
    }

    fun getUserStreak(): Flow<UserStreak?> = streakDao.getUserStreakFlow()

    private suspend fun updateStreak() {
        val currentStreak = streakDao.getUserStreak() ?: UserStreak()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        val lastCompletion = currentStreak.lastCompletionDate
        val newStreak = when {
            lastCompletion == null -> 1
            isSameDay(lastCompletion, today) -> currentStreak.currentStreak
            isConsecutiveDay(lastCompletion, today) -> currentStreak.currentStreak + 1
            else -> 1
        }

        val newLongestStreak = maxOf(currentStreak.longestStreak, newStreak)

        streakDao.insertOrUpdateStreak(
            currentStreak.copy(
                currentStreak = newStreak,
                longestStreak = newLongestStreak,
                lastCompletionDate = today,
                totalDaysCompleted = currentStreak.totalDaysCompleted + 1,
                updatedAt = Date()
            )
        )
    }

    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isConsecutiveDay(lastDate: Date, today: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = lastDate }
        val cal2 = Calendar.getInstance().apply { time = today }

        cal1.add(Calendar.DAY_OF_YEAR, 1)
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    fun getAllAchievements(): Flow<List<Achievement>> = achievementDao.getAllAchievements()

    private suspend fun checkAndUnlockAchievements() {
        val streak = streakDao.getUserStreak() ?: return

        when (streak.currentStreak) {
            1 -> unlockAchievement(
                AchievementType.FIRST_DAY,
                "اليوم الأول!",
                "لقد أكملت يومك الأول في حفظ القرآن الكريم بنجاح",
                streak.currentStreak
            )

            7 -> unlockAchievement(
                AchievementType.WEEK_STREAK,
                "محارب الأسبوع",
                "لقد أكملت 7 أيام في حفظ القرآن الكريم. تهانينا",
                streak.currentStreak
            )

            30 -> unlockAchievement(
                AchievementType.MONTH_STREAK,
                "مبدع هذا الشهر!",
                "ثلاثين يوماً من التفاني في حفظ القرآن الكريم",
                streak.currentStreak
            )
        }

        if (streak.totalVersesMemorized >= 100) {
            val existingAchievements =
                achievementDao.getAchievementsByType(AchievementType.HUNDRED_VERSES)
            if (existingAchievements.isEmpty()) {
                unlockAchievement(
                    AchievementType.HUNDRED_VERSES,
                    "مستكشف القرن!",
                    "لقد أكملت حفظ مائة آية. تهانينا",
                    streak.totalVersesMemorized
                )
            }
        }
    }

    private suspend fun unlockAchievement(
        type: AchievementType,
        title: String,
        description: String,
        value: Int,
    ) {
        val existing = achievementDao.getAchievementsByType(type)
        if (existing.none { it.value == value }) {
            val achievement = Achievement(
                type = type,
                title = title,
                description = description,
                unlockedAt = Date(),
                value = value
            )
            achievementDao.insertAchievement(achievement)
        }
    }

    suspend fun getScheduleVerseProgress(scheduleId: Long): ScheduleVerseProgress {
        val totalVerses = dailyTargetDao.getTotalVersesForSchedule(scheduleId)
        val completedVerses = dailyTargetDao.getCompletedVersesForSchedule(scheduleId)
        val totalTargets = dailyTargetDao.getTotalTargetsCount(scheduleId)
        val completedTargets = dailyTargetDao.getCompletedTargetsCount(scheduleId)
        val percentage = if (totalVerses > 0) {
            (completedVerses * 100) / totalVerses
        } else {
            0
        }

        return ScheduleVerseProgress(
            completedVerses = completedVerses,
            totalVerses = totalVerses,
            progressPercentage = percentage,
            completedTargets = completedTargets,
            totalTargets = totalTargets
        )
    }

    suspend fun getScheduleProgress(scheduleId: Long): ScheduleProgress {
        val verseProgress = getScheduleVerseProgress(scheduleId)
        return ScheduleProgress(
            completedTargets = verseProgress.completedTargets,
            totalTargets = verseProgress.totalTargets,
            progressPercentage = verseProgress.progressPercentage
        )
    }

    suspend fun getAchievementCount(): Int = achievementDao.getAchievementCount()


    suspend fun insertSchedule(schedule: MemorizationSchedule): Long {
        return scheduleDao.insertSchedule(schedule)
    }

    suspend fun insertDailyTargets(targets: List<DailyTarget>) {
        dailyTargetDao.insertTargets(targets)
    }

    suspend fun getDailyTargetsByScheduleId(scheduleId: Long): List<DailyTarget> {
        return dailyTargetDao.getTargetsForScheduleSync(scheduleId)
    }

    suspend fun getScheduleById(scheduleId: Long): MemorizationSchedule? {
        return scheduleDao.getScheduleById(scheduleId)
    }

    suspend fun insertSession(session: MemorizationSession): Long {
        return sessionDao.insertSession(session)
    }

    suspend fun getSessionById(sessionId: Long): MemorizationSession? {
        return sessionDao.getSessionById(sessionId)
    }

    suspend fun updateSession(session: MemorizationSession) {
        sessionDao.updateSession(session)
    }

    suspend fun getDailyTargetByDate(date: Date): DailyTarget? {
        val currentSchedule = scheduleDao.getCurrentActiveSchedule()
        return currentSchedule?.let { schedule ->
            dailyTargetDao.getTargetForDate(date, schedule.id)
        }
    }

    suspend fun updateDailyTarget(target: DailyTarget) {
        dailyTargetDao.updateTarget(target)
    }

    suspend fun insertOrUpdateUserStreak(streak: UserStreak) {
        streakDao.insertOrUpdateStreak(streak)
    }

    suspend fun getCurrentActiveScheduleSync(): MemorizationSchedule? {
        return scheduleDao.getCurrentActiveSchedule()
    }
    suspend fun deleteDailyTargetsForSchedule(scheduleId: Long) {
        dailyTargetDao.deleteTargetsForSchedule(scheduleId)
    }
}


data class WeeklyStats(
    val totalStudyTime: Int,
    val completedDays: Int,
    val currentStreak: Int,
    val targetsCompleted: Int,
    val partialTargetsWorkedOn: Int = 0,
    val totalVersesMemorized: Int = 0
)

data class ScheduleVerseProgress(
    val completedVerses: Int,
    val totalVerses: Int,
    val progressPercentage: Int,
    val completedTargets: Int,
    val totalTargets: Int,
)

data class ScheduleProgress(
    val completedTargets: Int,
    val totalTargets: Int,
    val progressPercentage: Int,
    val partiallyCompletedTargets: Int = 0
)

data class DailyTargetProgress(
    val target: DailyTarget,
    val totalVerses: Int,
    val completedVerses: Int,
    val progressPercentage: Int,
    val isCompleted: Boolean,
    val wasWorkedOnToday: Boolean = false
)