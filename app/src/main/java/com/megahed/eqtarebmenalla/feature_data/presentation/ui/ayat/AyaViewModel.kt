package com.megahed.eqtarebmenalla.feature_data.presentation.ui.ayat

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.repository.AyaRepository
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AyaViewModel@Inject constructor(
    private val ayaRepository: AyaRepository
) : ViewModel() {

     fun insertAya(aya: Aya){
         viewModelScope.launch {
             ayaRepository.insertAya(aya)
         }
    }

     fun updateAya(aya: Aya){
         viewModelScope.launch {
             ayaRepository.updateAya(aya)
         }
     }

     fun deleteAya(aya: Aya){
         viewModelScope.launch {
             ayaRepository.deleteAya(aya)
         }
     }

    suspend fun getAyaById(id:Int): Aya?{
        return ayaRepository.getAyaById(id)
    }

     fun getAyaOfSoraId(id:Int): Flow<List<Aya>> {
        return ayaRepository.getAyaOfSoraId(id)
    }


}