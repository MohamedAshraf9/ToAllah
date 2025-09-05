package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.SessionType
import com.megahed.eqtarebmenalla.db.model.UserStreak
import com.megahed.eqtarebmenalla.feature_data.data.repository.MemorizationRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.WeeklyStats
import com.megahed.eqtarebmenalla.feature_data.states.MemorizationUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class MemorizationViewModel @Inject constructor(
    private val memorizationRepository: MemorizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MemorizationUiState())
    val uiState = _uiState.asStateFlow()

    val currentSchedule = memorizationRepository.getCurrentActiveSchedule()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val todayTarget = memorizationRepository.getTodayTargetFlow()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val userStreak = memorizationRepository.getUserStreak()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserStreak()
        )

    val achievements = memorizationRepository.getAllAchievements()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        loadScheduleProgress()
        refreshTodayTarget()
    }
    private fun refreshTodayTarget() {
        viewModelScope.launch {
            try {
                val target = memorizationRepository.getTodayTarget()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load today's target: ${e.message}"
                )
            }
        }
    }

    fun createSchedule(
        title: String,
        description: String?,
        startDate: Date,
        endDate: Date,
        dailyTargets: List<DailyTarget>
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
                    message = "تم إنشاء جدول الحفظ بنجاح!",
                    lastCreatedScheduleId = scheduleId
                )

                loadScheduleProgress()
                refreshTodayTarget()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "فشل في إنشاء الجدول: ${e.message}"
                )
            }
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch {
            try {
                memorizationRepository.deleteSchedule(scheduleId)
                _uiState.value = _uiState.value.copy(message = "Schedule deleted successfully!")
                loadScheduleProgress()
                refreshTodayTarget()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to delete schedule: ${e.message}"
                )
            }
        }
    }

    fun markTodayTargetCompleted() {
        viewModelScope.launch {
            try {
                val today = todayTarget.value
                today?.let {
                    if (!it.isCompleted) {
                        memorizationRepository.markTargetCompleted(it.id)
                        _uiState.value = _uiState.value.copy(
                            message = "Great job! Today's target completed!",
                            showCelebration = shouldShowCelebration()
                        )
                        loadScheduleProgress()
                        refreshTodayTarget()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to mark target completed: ${e.message}"
                )
            }
        }
    }

    private suspend fun shouldShowCelebration(): Boolean {
        val streak = memorizationRepository.getUserStreak().first()
        return when (streak?.currentStreak) {
            1, 7, 30 -> true
            else -> streak?.currentStreak?.let { it > 0 && it % 10 == 0 } ?: false
        }
    }

    fun startMemorizationSession(sessionType: SessionType = SessionType.LISTENING) {
        viewModelScope.launch {
            try {
                val today = todayTarget.value
                today?.let {
                    val sessionId = memorizationRepository.startMemorizationSession(it.id, sessionType)
                    _uiState.value = _uiState.value.copy(
                        currentSessionId = sessionId,
                        isSessionActive = true
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to start session: ${e.message}"
                )
            }
        }
    }

    fun completeSession(versesCompleted: Int, notes: String? = null) {
        viewModelScope.launch {
            try {
                val sessionId = _uiState.value.currentSessionId
                sessionId?.let {
                    memorizationRepository.completeSession(it, versesCompleted, notes)
                    _uiState.value = _uiState.value.copy(
                        currentSessionId = null,
                        isSessionActive = false,
                        message = "Session completed successfully!"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to complete session: ${e.message}"
                )
            }
        }
    }

    fun loadScheduleProgress() {
        viewModelScope.launch {
            try {
                val schedule = currentSchedule.value
                schedule?.let {
                    val targetProgress = memorizationRepository.getScheduleProgress(it.id)
                    val verseProgress = memorizationRepository.getScheduleVerseProgress(it.id)

                    _uiState.value = _uiState.value.copy(
                        scheduleProgress = targetProgress,
                        verseProgress = verseProgress
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = "Failed to load progress: ${e.message}"
                )
            }
        }
    }
    suspend fun getWeeklyStats(): WeeklyStats {
        return try {
            val totalStudyTime = memorizationRepository.getTotalStudyTimeThisWeek()
            val streak = userStreak.value
            val schedule = currentSchedule.value

            val completedThisWeek = if (schedule != null) {
                val calendar = Calendar.getInstance()
                val endDate = calendar.time
                calendar.add(Calendar.DAY_OF_WEEK, -7)
                val startDate = calendar.time

                memorizationRepository.getCompletedTargetsInRange(schedule.id, startDate, endDate)
            } else 0

            WeeklyStats(
                totalStudyTime = totalStudyTime,
                completedDays = completedThisWeek,
                currentStreak = streak?.currentStreak ?: 0,
                targetsCompleted = _uiState.value.scheduleProgress?.completedTargets ?: 0
            )
        } catch (e: Exception) {
            WeeklyStats(0, 0, 0, 0)
        }
    }

    fun refreshAllData() {
        viewModelScope.launch {
            loadScheduleProgress()
            refreshTodayTarget()
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

    fun clearLastCreatedScheduleId() {
        _uiState.value = _uiState.value.copy(lastCreatedScheduleId = null)
    }
}