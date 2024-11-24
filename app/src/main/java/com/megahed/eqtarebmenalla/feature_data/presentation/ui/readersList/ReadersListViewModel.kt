package com.megahed.eqtarebmenalla.feature_data.presentation.ui.readersList

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.ReadersWithAyatTimingsUseCase
import com.megahed.eqtarebmenalla.feature_data.states.ReadersState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

@HiltViewModel
class ReadersListViewModel @Inject constructor(
    private val readersUseCase: ReadersWithAyatTimingsUseCase
): ViewModel() {

    private val _readersState = MutableStateFlow(ReadersState())
    val readersState: StateFlow<ReadersState> = _readersState

    fun getReadersWithAyatTimings(soraId: Int?) {
        readersUseCase().onEach {
            when(it){
                is Resource.Success ->{
                    it.data?.let{
                        val readers = it.filter { it.suras.isNotEmpty() }
                        _readersState.value= ReadersState(readers = readers)
                    }


                }
                is Resource.Loading ->{
                    _readersState.value= ReadersState(isLoading = true)
                }
                is Resource.Error ->{
                    _readersState.value= ReadersState(error = it.message?:"error")
                }
            }
        }.launchIn(viewModelScope)
    }
}