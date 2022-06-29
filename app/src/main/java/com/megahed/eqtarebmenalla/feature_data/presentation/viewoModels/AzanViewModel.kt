package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.AzanInfoDto
import com.megahed.eqtarebmenalla.feature_data.domain.model.DataMaps
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.GetAzanDataUsesCase
import com.megahed.eqtarebmenalla.feature_data.presentation.AzanListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class AzanViewModel @Inject constructor(
    private val usesCase: GetAzanDataUsesCase
):ViewModel(){


    private val _state= MutableStateFlow(AzanListState())
    val state :StateFlow<AzanListState> =_state



    fun getAzanData(city:String,country:String){
        usesCase(city,country).onEach {
            when(it){
                is Resource.Success ->{
                    _state.value= AzanListState(azanInfoDto = it.data?: emptyList())
                }
                is Resource.Loading ->{
                    _state.value= AzanListState(isLoading = true)
                }
                is Resource.Error ->{
                    _state.value=AzanListState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)
    }

}