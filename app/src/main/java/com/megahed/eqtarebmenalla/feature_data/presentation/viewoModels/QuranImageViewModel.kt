package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.exoplayer.State
import com.megahed.eqtarebmenalla.feature_data.states.AyaHefzState
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.QuranImageUsesCase
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.ReadersWithAyatTimingsUseCase
import com.megahed.eqtarebmenalla.feature_data.states.QuranImageState
import com.megahed.eqtarebmenalla.feature_data.states.QuranListenerListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class QuranImageViewModel @Inject constructor(
    private val usesCase: QuranImageUsesCase
):ViewModel(){

    private val _state= MutableStateFlow(QuranImageState())
    val state :StateFlow<QuranImageState> =_state

    suspend fun getQuranImage(surah:Int, read:Int=9){
        _state.value = _state.value.copy(isLoading = true)
        usesCase(surah, read).collect { resource ->
            _state.value = when (resource) {
                is Resource.Success -> {
                    _state.value.copy(
                        quranImage = resource.data ?: emptyList(),
                        isLoading = false
                    )
                }
                is Resource.Loading -> {
                    _state.value.copy(
                        isLoading = true
                    )
                }
                is Resource.Error -> {
                    _state.value.copy(
                        isLoading = false,
                        error = resource.message?:"error"
                    )
                }
                }
            }


    }
}