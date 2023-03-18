package com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir



import com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir.entity.AyaTafsir
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitServiceTafsir {


        @GET("1/{suraId}/{ayaId}")
        fun getTafsirl(@Path("suraId") suraId : String, @Path("ayaId") ayaId : String) : Call<AyaTafsir>


}