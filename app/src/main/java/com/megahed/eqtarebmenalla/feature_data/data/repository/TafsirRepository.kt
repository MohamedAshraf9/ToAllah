package com.megahed.eqtarebmenalla.feature_data.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.RetrofitHelper
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.ResultHefz
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.RetrofitHelperQibla
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity.Qibla
import com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir.RetrofitHelperTafsir
import com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir.entity.AyaTafsir
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TafsirRepository {

    fun getTafsir(suraId :String, ayaId :String) : LiveData<AyaTafsir> {
        val mutableLiveData = MutableLiveData<AyaTafsir>()

        RetrofitHelperTafsir.api.getTafsirl(suraId, ayaId).clone().enqueue(object :
            Callback<AyaTafsir>{
            override fun onResponse(
                call: Call<AyaTafsir>,
                response: Response<AyaTafsir>
            ) {
                Log.println(Log.ASSERT, "Rewat list : ", response.body().toString())
                mutableLiveData.value = response.body()
            }

            override fun onFailure(call: Call<AyaTafsir>, t: Throwable) {
                Log.println(Log.ASSERT, "Failed : ", t.message.toString())
            }

        })

        return mutableLiveData
    }

}