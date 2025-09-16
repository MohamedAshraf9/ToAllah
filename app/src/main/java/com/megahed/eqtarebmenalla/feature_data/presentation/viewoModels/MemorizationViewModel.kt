package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.db.model.SessionType
import com.megahed.eqtarebmenalla.db.model.UserStreak
import com.megahed.eqtarebmenalla.db.model.getTotalVerses
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.DailyTargetProgress
import com.megahed.eqtarebmenalla.feature_data.data.repository.MemorizationRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.ScheduleProgress
import com.megahed.eqtarebmenalla.feature_data.data.repository.WeeklyStats
import com.megahed.eqtarebmenalla.feature_data.states.MemorizationUiState
import com.megahed.eqtarebmenalla.feature_data.states.VerseProgress
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val memorizationRepository: MemorizationRepository,
    private val offlineAudioManager: OfflineAudioManager,
    private val quranListenerReaderRepository: QuranListenerReaderRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState: StateFlow<MemorizationUiState> = _uiState.asStateFlow()

    private val _currentSchedule = MutableStateFlow<MemorizationSchedule?>(null)
    val currentSchedule: StateFlow<MemorizationSchedule?> = _currentSchedule.asStateFlow()

    private val _todayTarget = MutableStateFlow<DailyTarget?>(null)
    val todayTarget: StateFlow<DailyTarget?> = _todayTarget.asStateFlow()

    private val _userStreak = MutableStateFlow<UserStreak?>(null)
    val userStreak: StateFlow<UserStreak?> = _userStreak.asStateFlow()

    private val _todayTargetProgress = MutableStateFlow<DailyTargetProgress?>(null)
    val todayTargetProgress: StateFlow<DailyTargetProgress?> = _todayTargetProgress.asStateFlow()

    init {
        loadCurrentSchedule()
        loadTodayTarget()
        loadUserStreak()
    }

    suspend fun getScheduleProgress(scheduleId: Long): ScheduleProgress {
        return memorizationRepository.getScheduleProgress(scheduleId)
    }

    suspend fun getScheduleWithTargets(scheduleId: Long): Pair<MemorizationSchedule?, List<DailyTarget>> {
        return try {
            val schedule = memorizationRepository.getScheduleById(scheduleId)
            val targets = if (schedule != null) {
                memorizationRepository.getDailyTargetsByScheduleId(scheduleId)
            } else {
                emptyList()
            }
            Pair(schedule, targets)
        } catch (e: Exception) {
            Pair(null, emptyList())
        }
    }

    fun updateScheduleWithTargets(
        schedule: MemorizationSchedule,
        newTargets: List<DailyTarget>,
    ) = viewModelScope.launch {
        try {
            _uiState.value = _uiState.value.copy(isLoading = true)

            memorizationRepository.updateSchedule(schedule)

            val oldTargets = memorizationRepository.getDailyTargetsByScheduleId(schedule.id)

            val mergedTargets = newTargets.map { newTarget ->
                val matchingOld = oldTargets.find { old ->

                    old.targetDate == newTarget.targetDate && old.surahId == newTarget.surahId
                }

                if (matchingOld != null) {
                    val newTotal = newTarget.getTotalVerses()
                    val completed = matchingOld.completedVerses

                    if (completed >= newTotal) {
                        newTarget.copy(
                            scheduleId = schedule.id,
                            completedVerses = newTotal,
                            isCompleted = true
                        )
                    } else {
                        newTarget.copy(
                            scheduleId = schedule.id,
                            completedVerses = completed
                        )
                    }
                } else {
                    newTarget.copy(scheduleId = schedule.id)
                }

            }

            memorizationRepository.deleteDailyTargetsForSchedule(schedule.id)
            memorizationRepository.insertDailyTargets(mergedTargets)

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                message = "تم تحديث جدول الحفظ بنجاح",
                lastCreatedScheduleId = schedule.id
            )

            refreshAllData()

        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "فشل في تحديث جدول الحفظ: ${e.message}"
            )
        }
    }

    fun updateVerseProgress(versesCompleted: Int) {
        viewModelScope.launch {
            try {
                val todayTarget = _todayTarget.value
                if (todayTarget != null) {
                    memorizationRepository.updateVerseProgress(
                        targetId = todayTarget.id,
                        completedVerses = versesCompleted
                    )

                    loadTodayTargetProgress()
                    refreshAllData()

                    val totalVerses = todayTarget.getTotalVerses()
                    if (versesCompleted >= totalVerses) {
                        _uiState.value = _uiState.value.copy(
                            showCelebration = true,
                            message = "تهانينا! لقد أكملت هدف اليوم بنجاح!"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "فشل في حفظ التقدم: ${e.message}"
                )
            }
        }
    }

    fun completeSession(versesCompleted: Int, notes: String?, markAsCompleted: Boolean = false) {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSessionId
                if (sessionId != null) {
                    memorizationRepository.completeSession(
                        sessionId = sessionId,
                        versesCompleted = versesCompleted,
                        notes = notes
                    )

                    val todayTarget = _todayTarget.value
                    if (todayTarget != null) {
                        if (markAsCompleted) {
                            memorizationRepository.markTargetCompleted(todayTarget.id)
                        } else {
                            val currentProgress =
                                maxOf(todayTarget.completedVerses, versesCompleted)
                            memorizationRepository.updateVerseProgress(
                                todayTarget.id,
                                currentProgress
                            )
                        }

                        loadTodayTargetProgress()

                        val totalVerses = todayTarget.getTotalVerses()
                        val completionMessage =
                            if (markAsCompleted || versesCompleted >= totalVerses) {
                                "تهانينا! لقد أكملت هدف اليوم بنجاح!"
                            } else {
                                "تم حفظ تقدمك: ${versesCompleted} من $totalVerses آية"
                            }

                        _uiState.value = _uiState.value.copy(
                            isSessionActive = false,
                            currentSessionId = null,
                            message = completionMessage,
                            showCelebration = markAsCompleted || versesCompleted >= totalVerses
                        )
                    }
                } else {
                    _uiState.value = _uiState.value.copy(error = "لا يوجد جلسات حفظ نشطة")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "فشل في إكمال الجلسة: ${e.message}")
            }
        }
    }

    fun loadTodayTargetProgress() {
        viewModelScope.launch {
            try {
                val progress = memorizationRepository.getTodayTargetProgress()
                _todayTargetProgress.value = progress
            } catch (e: Exception) {

            }
        }
    }


    private fun loadTodayTarget() {
        viewModelScope.launch {
            try {
                memorizationRepository.getTodayTargetFlow().collect { target ->
                    _todayTarget.value = target
                    if (target != null) {
                        loadTodayTargetProgress()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "فشل في تحميل هدف اليوم: ${e.message}")
            }
        }
    }

    private fun loadCurrentSchedule() {
        viewModelScope.launch {
            try {
                memorizationRepository.getCurrentActiveSchedule().collect { schedule ->
                    _currentSchedule.value = schedule
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "فشل في تحميل الجدول: ${e.message}")
            }
        }
    }

    private fun loadUserStreak() {
        viewModelScope.launch {
            try {
                memorizationRepository.getUserStreak().collect { streak ->
                    _userStreak.value = streak
                }
            } catch (e: Exception) {
            }
        }
    }

    fun createSchedule(
        title: String,
        description: String,
        startDate: Date,
        endDate: Date,
        dailyTargets: List<DailyTarget>,
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val scheduleId = memorizationRepository.createSchedule(
                    title = title,
                    description = description,
                    startDate = startDate,
                    endDate = endDate,
                    dailyTargets = dailyTargets
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "تم إنشاء جدول الحفظ بنجاح",
                    lastCreatedScheduleId = scheduleId
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "فشل إنشاء جدول الحفظ ${e.message}"
                )
            }
        }
    }

    fun downloadScheduleForOffline(
        schedule: MemorizationSchedule,
        readerId: String,
        readerName: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> },
    ) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)

                val dailyTargets = memorizationRepository.getDailyTargetsByScheduleId(schedule.id)

                val uniqueSurahs = dailyTargets.map { it.surahId }.distinct()
                var downloadedCount = 0

                uniqueSurahs.forEach { surahId ->
                    val surahName = Constants.SORA_OF_QURAN[surahId] ?: "Surah $surahId"

                    val isDownloaded = offlineAudioManager.isAudioDownloaded(readerId, surahId)
                    if (!isDownloaded) {
                        val audioUrl = constructAudioUrlForReader(readerId, surahId)

                        val success = offlineAudioManager.downloadAudio(
                            readerId = readerId,
                            surahId = surahId,
                            surahName = surahName,
                            readerName = readerName,
                            audioUrl = audioUrl
                        )

                        if (success) {
                            downloadedCount++
                            onProgress(downloadedCount, uniqueSurahs.size)
                        }
                    } else {
                        downloadedCount++
                        onProgress(downloadedCount, uniqueSurahs.size)
                    }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    message = "تم تحميل محتوى الجدول بنجاح للحفظ بدون إنترنت!"
                )

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "فشل تحميل الجدول: ${e.message}"
                )
            }
        }
    }

    private suspend fun constructAudioUrlForReader(readerId: String, surahId: Int): String {
        return try {
            val normalizedReaderId = normalizeToAsciiDigits(readerId)
            val reader =
                quranListenerReaderRepository.getQuranListenerReaderById(normalizedReaderId)
            reader?.let { quranReader ->
                Constants.getSoraLink(quranReader.server, surahId)
            } ?: throw Exception("Reader not found")
        } catch (e: Exception) {
            Log.e("MemorizationViewModel", "Failed to construct URL", e)
            val normalizedReaderId = normalizeToAsciiDigits(readerId)
            "https://www.mp3quran.net/api/reader/$normalizedReaderId/${
                String.format(
                    Locale.US,
                    "%03d",
                    surahId
                )
            }.mp3"
        }
    }

    private fun normalizeToAsciiDigits(input: String): String {
        return input.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }

    fun startMemorizationSession(sessionType: SessionType) {
        viewModelScope.launch {
            try {
                val todayTarget = _todayTarget.value
                if (todayTarget != null) {
                    val sessionId = memorizationRepository.startMemorizationSession(
                        dailyTargetId = todayTarget.id,
                        sessionType = sessionType
                    )

                    _uiState.value = _uiState.value.copy(
                        isSessionActive = true,
                        currentSessionId = sessionId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(error = "لم يتم العثور على هدف لهذا اليوم")
                }

            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "فشل في بدء جلسة الحفظ: ${e.message}")
            }
        }
    }

    fun completeSession(versesCompleted: Int, notes: String?) {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSessionId
                if (sessionId != null) {
                    memorizationRepository.completeSession(
                        sessionId = sessionId,
                        versesCompleted = versesCompleted,
                        notes = notes
                    )

                    _uiState.value = _uiState.value.copy(
                        isSessionActive = false,
                        currentSessionId = null,
                        message = "تهانينا لقد أكملت جلسة الحفظ بنجاح!"
                    )

                } else {
                    _uiState.value = _uiState.value.copy(error = "لا يوجد جلسات حفظ نشطة")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "فشل تحميل الجلسة: ${e.message}")
            }
        }
    }

    fun markTodayTargetCompleted() {
        viewModelScope.launch {
            try {
                _todayTarget.value?.let { target ->
                    memorizationRepository.markTargetCompleted(target.id)

                    _uiState.value = _uiState.value.copy(
                        showCelebration = true,
                        message = "أحسنت لقد أكلمت هدفك بنجاح!"
                    )
                } ?: run {
                    _uiState.value = _uiState.value.copy(error = "لم يتم العثور على هدف لليوم")
                }
            } catch (e: Exception) {
                _uiState.value =
                    _uiState.value.copy(error = "فشل في تحديد هدف اليوم كهدف مكتمل: ${e.message}")
            }
        }
    }

    fun loadScheduleProgress() {
        viewModelScope.launch {
            try {
                _currentSchedule.value?.let { schedule ->
                    val progress = memorizationRepository.getScheduleProgress(schedule.id)
                    _uiState.value = _uiState.value.copy(scheduleProgress = progress)

                    val verseProgress = memorizationRepository.getScheduleVerseProgress(schedule.id)
                    _uiState.value = _uiState.value.copy(
                        verseProgress = VerseProgress(
                            completedVerses = verseProgress.completedVerses,
                            totalVerses = verseProgress.totalVerses,
                            progressPercentage = verseProgress.progressPercentage
                        )
                    )
                }
            } catch (e: Exception) {
            }
        }
    }

    fun refreshAllData() {
        loadCurrentSchedule()
        loadTodayTarget()
        loadUserStreak()
        loadScheduleProgress()
        loadTodayTargetProgress()
    }

    suspend fun getWeeklyStats(): WeeklyStats {
        return try {
            val currentSchedule = _currentSchedule.value
            val totalStudyTime = memorizationRepository.getTotalStudyTimeThisWeek()
            val currentStreak = _userStreak.value?.currentStreak ?: 0

            val calendar = Calendar.getInstance()
            val endDate = calendar.time
            calendar.add(Calendar.DAY_OF_WEEK, -7)
            val startDate = calendar.time

            val completedTargetsThisWeek = if (currentSchedule != null) {
                memorizationRepository.getCompletedTargetsInRange(
                    currentSchedule.id,
                    startDate,
                    endDate
                )
            } else {
                0
            }

            val completedDays = minOf(currentStreak, 7)

            WeeklyStats(
                totalStudyTime = totalStudyTime,
                completedDays = completedDays,
                currentStreak = currentStreak,
                targetsCompleted = completedTargetsThisWeek
            )
        } catch (e: Exception) {
            WeeklyStats(
                totalStudyTime = 0,
                completedDays = 0,
                currentStreak = 0,
                targetsCompleted = 0
            )
        }
    }

    fun dismissMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun dismissCelebration() {
        _uiState.value = _uiState.value.copy(showCelebration = false)
    }

    suspend fun getScheduleById(scheduleId: Long): MemorizationSchedule? =
        memorizationRepository.getScheduleById(scheduleId)

    fun updateSchedule(schedule: MemorizationSchedule) = viewModelScope.launch {
        memorizationRepository.updateSchedule(schedule)
    }
}