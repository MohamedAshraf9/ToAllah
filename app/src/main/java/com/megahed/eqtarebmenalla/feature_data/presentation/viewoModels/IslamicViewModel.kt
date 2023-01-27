package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.PrayerDataUsesCase
import com.megahed.eqtarebmenalla.feature_data.presentation.IslamicListState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.http.Query
import javax.inject.Inject

@HiltViewModel
class IslamicViewModel @Inject constructor(
    private val usesCase: PrayerDataUsesCase
):ViewModel(){


    private val _state= MutableStateFlow(IslamicListState())
    val state :StateFlow<IslamicListState> =_state


    //private val _state1= MutableStateFlow(IslamicInfo(1,"dsdsd", DataDto()))
    //val state1 :StateFlow<IslamicInfo> =_state1


    fun getAzanData(@Query("latitude") latitude:Double,
                    @Query("longitude") longitude:Double){
        usesCase(latitude, longitude).onEach {
            when(it){
                is Resource.Success ->{
                    _state.value= it.data?.let { it1 -> IslamicListState(islamicInfo = it1) }!!

                }
                is Resource.Loading ->{
                    _state.value= IslamicListState(isLoading = true)
                }
                is Resource.Error ->{
                    _state.value=IslamicListState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)
    }

}