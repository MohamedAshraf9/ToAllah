package com.megahed.eqtarebmenalla.feature_data.data.remote.hez


import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.ResultHefz
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.SuraMp3
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitService {

        @GET("v1/edition/format/audio")
        fun getAllRewat() : Call<ResultHefz>

        @GET("v1/surah/{idSura}/{qar2e}")
        fun getSuraMp3(@Path("idSura") idSura : Int, @Path("qar2e") qar2e : String ) : Call<SuraMp3>


}