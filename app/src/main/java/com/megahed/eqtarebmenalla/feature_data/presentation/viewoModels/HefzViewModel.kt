package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import com.megahed.eqtarebmenalla.feature_data.AyaHefzState
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.toQuranListenerReader
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.PrayerDataUsesCase
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.QuranListenerUsesCase
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.VerseReaderUsesCase
import com.megahed.eqtarebmenalla.feature_data.presentation.IslamicListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.http.Query
import javax.inject.Inject

@HiltViewModel
class HefzViewModel @Inject constructor(
    private val usesCase: VerseReaderUsesCase
):ViewModel(){


    private val _state= MutableStateFlow(AyaHefzState())
    val state :StateFlow<AyaHefzState> =_state




    init {
        usesCase().onEach {
            when(it){
                is Resource.Success ->{
                    it.data?.let{
                        _state.value= AyaHefzState(recitersVerse =  it.reciters_verse)
                    }


                }
                is Resource.Loading ->{
                    _state.value= AyaHefzState(isLoading = true)
                }
                is Resource.Error ->{
                    _state.value=AyaHefzState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)

    }

}