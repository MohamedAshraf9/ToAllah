package com.megahed.eqtarebmenalla.feature_data.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.RetrofitHelperQibla

import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity.Qibla
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class QiblaRepository {

        fun getQibla(latitude :String, longitude: String) : LiveData<Qibla>{
           val mutableLiveData = MutableLiveData<Qibla>()

                RetrofitHelperQibla.api.getQiblaAngl(latitude, longitude).clone().enqueue(object :
                    Callback<Qibla>{
                    override fun onResponse(
                        call: Call<Qibla>,
                        response: Response<Qibla>
                    ) {
                        Log.println(Log.ASSERT, "Qibla : ", response.body().toString())
                        mutableLiveData.value = response.body()
                    }

                    override fun onFailure(call: Call<Qibla>, t: Throwable) {
                        Log.println(Log.ASSERT, "Failed : ", t.message.toString())
                    }

                })

           return mutableLiveData
       }



}