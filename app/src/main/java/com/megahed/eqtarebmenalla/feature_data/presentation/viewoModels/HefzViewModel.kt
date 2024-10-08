package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.states.AyaHefzState
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.VerseReaderUsesCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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
                    _state.value= AyaHefzState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)

    }

}