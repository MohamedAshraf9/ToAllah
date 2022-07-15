package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListener

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.QuranListenerUsesCase
import com.megahed.eqtarebmenalla.feature_data.presentation.IslamicListState
import com.megahed.eqtarebmenalla.feature_data.presentation.QuranListenerListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class QuranListenerViewModel @Inject constructor(
    private val usesCase: QuranListenerUsesCase
) : ViewModel() {


    private val _state= MutableStateFlow(QuranListenerListState())
    val state : StateFlow<QuranListenerListState> =_state


    fun getQuranData(){
        usesCase().onEach {
            when(it){
                is Resource.Success ->{
                    _state.value= it.data?.let { it1 -> QuranListenerListState(reciter = it1.reciters) }!!

                }
                is Resource.Loading ->{
                    _state.value= QuranListenerListState(isLoading = true)
                }
                is Resource.Error ->{
                    _state.value=QuranListenerListState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)
    }



}