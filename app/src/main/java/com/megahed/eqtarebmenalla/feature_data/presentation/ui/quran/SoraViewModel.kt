package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SoraViewModel@Inject constructor(
    private val soraRepository: SoraRepository
) : ViewModel() {


     fun insertSora(sora: Sora){
        viewModelScope.launch {
            soraRepository.insertSora(sora)
        }
    }

     fun updateSora(sora: Sora){
         viewModelScope.launch {
             soraRepository.updateSora(sora)
         }
    }

     fun deleteSora(sora: Sora){
         viewModelScope.launch {
             soraRepository.deleteSora(sora)
         }
    }

     suspend fun getSoraById(id:Int): Sora?{
        return soraRepository.getSoraById(id)

    }

    fun getAllSora(): Flow<List<Sora>>{
        return soraRepository.getAllSora()
    }



}