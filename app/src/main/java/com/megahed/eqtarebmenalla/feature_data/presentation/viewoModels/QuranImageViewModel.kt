package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.states.AyaHefzState
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.QuranImageUsesCase
import com.megahed.eqtarebmenalla.feature_data.states.QuranImageState
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



    fun getQuranImage(surah:Int, read:Int=9){
        usesCase(surah, read).onEach {
            when(it){
                is Resource.Success ->{
                    it.data?.let{
                        _state.value= QuranImageState(quranImage =  it)
                    }


                }
                is Resource.Loading ->{
                    _state.value= QuranImageState(isLoading = true)
                }
                is Resource.Error ->{
                    _state.value= QuranImageState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)


    }




}