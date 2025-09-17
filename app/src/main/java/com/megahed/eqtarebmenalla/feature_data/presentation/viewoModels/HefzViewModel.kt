package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.MethodHelper
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.db.dao.CachedRecitersDao
import com.megahed.eqtarebmenalla.db.model.CachedReciter
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.RecitersVerse
import com.megahed.eqtarebmenalla.feature_data.states.AyaHefzState
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.VerseReaderUsesCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.collections.map

@HiltViewModel
class HefzViewModel @Inject constructor(
    private val usesCase: VerseReaderUsesCase,
    private val cachedRecitersDao: CachedRecitersDao,
    application: Application,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(AyaHefzState())
    val state: StateFlow<AyaHefzState> = _state

    init {
        loadReciters()
    }

    private fun loadReciters() {
        viewModelScope.launch {
            loadCachedReciters()

            if (isNetworkAvailable()) {
                fetchFreshReciters()
            } else {
                if (_state.value.recitersVerse.isEmpty()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "لا يوجد اتصال بالإنترنت أو بيانات تم تحميلها"
                    )
                }
            }
        }
    }

    private suspend fun loadCachedReciters() {
        try {
            val cachedReciters = cachedRecitersDao.getAllCachedReciters()

            if (cachedReciters.isNotEmpty()) {
                val recitersVerse = cachedReciters.map { cached ->
                    RecitersVerse(
                        id = cached.id.toString(),
                        name = cached.name,
                        audio_url_bit_rate_32_ = cached.audio_url_bit_rate_32_,
                        audio_url_bit_rate_64 = cached.audio_url_bit_rate_64,
                        audio_url_bit_rate_128 = cached.audio_url_bit_rate_128,
                        musshaf_type = "",
                        rewaya = ""
                    )
                }
                _state.value = _state.value.copy(
                    recitersVerse = recitersVerse,
                    isLoading = false,
                    error = ""
                )
            } else {
                if (isNetworkAvailable()) {
                    _state.value = _state.value.copy(isLoading = true, error = "")
                }
            }
        } catch (e: Exception) {
            if (!isNetworkAvailable()) {
                _state.value = _state.value.copy(
                    error = "خطأ في تحميل البيانات المحفوظة: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun fetchFreshReciters() {
        try {
            usesCase().collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.let { verseReaders ->

                            _state.value = _state.value.copy(
                                recitersVerse = verseReaders.reciters_verse,
                                isLoading = false,
                                error = ""
                            )

                            cacheReciters(verseReaders.reciters_verse)
                        }
                    }

                    is Resource.Loading -> {
                        if (_state.value.recitersVerse.isEmpty()) {
                            _state.value = _state.value.copy(isLoading = true, error = "")
                        }
                    }

                    is Resource.Error -> {
                        if (_state.value.recitersVerse.isEmpty()) {
                            _state.value = _state.value.copy(
                                error = result.message ?: "فشل تحميل القرآء",
                                isLoading = false
                            )
                        } else {
                            _state.value = _state.value.copy(isLoading = false)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            if (_state.value.recitersVerse.isEmpty()) {
                _state.value = _state.value.copy(
                    error = "Failed to fetch reciters: ${e.message}",
                    isLoading = false
                )
            }
        }
    }

    private suspend fun cacheReciters(reciters: List<RecitersVerse>) {
        try {
            val cachedReciters = reciters.map { reciter ->
                CachedReciter(
                    id = reciter.id.toInt(),
                    name = reciter.name,
                    audio_url_bit_rate_32_ = reciter.audio_url_bit_rate_32_,
                    audio_url_bit_rate_64 = reciter.audio_url_bit_rate_64,
                    audio_url_bit_rate_128 = reciter.audio_url_bit_rate_128
                )
            }

            cachedRecitersDao.clearCache()
            cachedRecitersDao.insertReciters(cachedReciters)
        } catch (_: Exception) {

        }
    }

    private fun isNetworkAvailable(): Boolean {
        val isOnline = MethodHelper.isOnline(getApplication<Application>().applicationContext)
        return isOnline
    }

    fun refreshReciters() {
        viewModelScope.launch {
            if (isNetworkAvailable()) {
                fetchFreshReciters()
            } else {
                loadCachedReciters()

                if (_state.value.recitersVerse.isEmpty()) {
                    _state.value = _state.value.copy(
                        error = "لا يوجد إتصال بالإنترنت. تحديث البيانات غير ممكن",
                        isLoading = false
                    )
                }
            }
        }
    }

}