package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quran

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.repository.AyaRepository
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@HiltViewModel
class AyaViewModel@Inject constructor(
    private val ayaRepository: AyaRepository
) : ViewModel() {

    suspend fun insertAya(aya: Aya){

    }

    suspend fun updateAya(aya: Aya){}

    suspend fun deleteAya(aya: Aya){}

    suspend fun getAyaById(id:Int): Aya?{
        return ayaRepository.getAyaById(id)
    }

     fun getAyaOfSoraId(id:Int): Flow<List<Aya>> {
        return ayaRepository.getAyaOfSoraId(id)
    }


}