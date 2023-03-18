package com.megahed.eqtarebmenalla.feature_data.data.remote.hez

import android.provider.SyncStateContract.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelper {
    private const val baseUrl = "https://api.alquran.cloud/"

    private val retrofit by lazy {



           Retrofit.Builder().baseUrl(com.megahed.eqtarebmenalla.common.Constants.QURAN_BY_EYA_BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                // we need to add converter factory to
                // convert JSON object to Java object
                .build()


    }
    val api : RetrofitService by lazy {
        retrofit.create(RetrofitService::class.java)
    }
}