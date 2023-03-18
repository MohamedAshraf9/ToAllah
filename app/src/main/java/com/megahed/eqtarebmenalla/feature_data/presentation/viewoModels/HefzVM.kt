package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import androidx.lifecycle.LiveData
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.ResultHefz
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.SuraMp3
import com.megahed.eqtarebmenalla.feature_data.data.repository.HefzRepository

class HefzVM {

    val hefzRepository : HefzRepository

    constructor(){
        hefzRepository = HefzRepository()
    }

    fun getAllRewat(): LiveData<ResultHefz>{

      return  hefzRepository.getAllRewat()

    }

    fun getSuraMp3(suraId : Int, qar2e : String): LiveData<SuraMp3>{
        return hefzRepository.getSuraMp3(suraId, qar2e)
    }
}