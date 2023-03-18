package com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir

import com.megahed.eqtarebmenalla.common.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitHelperTafsir {


    private val retrofit by lazy {



           Retrofit.Builder().baseUrl(Constants.TAFSIR)
                .addConverterFactory(GsonConverterFactory.create())

                .build()


    }
    val api : RetrofitServiceTafsir by lazy {
        retrofit.create(RetrofitServiceTafsir::class.java)
    }
}