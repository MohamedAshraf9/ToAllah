package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla

import com.megahed.eqtarebmenalla.common.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelperQibla {


    private val retrofit by lazy {



           Retrofit.Builder().baseUrl(Constants.PRAYER_TIME_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())

                .build()


    }
    val api : RetrofitServiceQibla by lazy {
        retrofit.create(RetrofitServiceQibla::class.java)
    }
}