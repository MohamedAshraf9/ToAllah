package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.LiveData
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity.Qibla
import com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir.entity.AyaTafsir
import com.megahed.eqtarebmenalla.feature_data.data.repository.QiblaRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.TafsirRepository

class TafsirVM {

    val tafsirRepository : TafsirRepository

    constructor(){
        tafsirRepository = TafsirRepository()
    }


    fun getTafsir(suraId :String, ayaId: String) : LiveData<AyaTafsir> {

        return tafsirRepository.getTafsir(suraId, ayaId)
    }

}