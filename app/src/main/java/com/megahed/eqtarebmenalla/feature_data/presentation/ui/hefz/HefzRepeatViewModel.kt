package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.megahed.eqtarebmenalla.db.model.Aya
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HefzRepeatViewModel @Inject constructor() : ViewModel() {

    private val _isMemorizationStopped = MutableLiveData<Boolean>().apply { value = false }
    val isMemorizationStopped: MutableLiveData<Boolean> = _isMemorizationStopped

    private val _currentAyaPosition = MutableLiveData<Int>().apply { value = 0 }
    val currentAyaPosition: MutableLiveData<Int> = _currentAyaPosition

    fun stopMemorization() {
        _isMemorizationStopped.value = true
    }

    fun resetMemorization() {
        _isMemorizationStopped.value = false
        _currentAyaPosition.value = 0
    }

    fun setCurrentAyaPosition(position: Int) {
        _currentAyaPosition.value = position
    }

    override fun onCleared() {
        super.onCleared()
        stopMemorization()
    }
}