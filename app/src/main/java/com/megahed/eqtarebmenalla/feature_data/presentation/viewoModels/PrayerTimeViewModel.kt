package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Insert
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PrayerTimeViewModel @Inject constructor(
    private val prayerTimeRepository: PrayerTimeRepository
):ViewModel() {


    fun insertPrayerTime(prayerTime: PrayerTime){
        viewModelScope.launch {
            prayerTimeRepository.insertPrayerTime(prayerTime)
        }
    }

     fun getPrayerTimeById(): Flow<PrayerTime?> {
        return prayerTimeRepository.getPrayerTimeById(1)
    }


}