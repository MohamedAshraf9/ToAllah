package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.LiveData
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity.Qibla
import com.megahed.eqtarebmenalla.feature_data.data.repository.HefzRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.QiblaRepository

class QiblaVM {

    val qiblaRepository : QiblaRepository

    constructor(){
        qiblaRepository = QiblaRepository()
    }

    fun getQibla(latitude :String, longitude: String) : LiveData<Qibla> {

        return qiblaRepository.getQibla(latitude, longitude)
    }
}