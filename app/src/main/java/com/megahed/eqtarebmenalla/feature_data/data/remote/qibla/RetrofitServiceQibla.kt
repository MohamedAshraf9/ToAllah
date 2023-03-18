package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla


import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.ResultHefz
import com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity.SuraMp3
import com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity.Qibla
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface RetrofitServiceQibla {


        @GET("v1/qibla/{latitude}/{longitude}")
        fun getQiblaAngl(@Path("latitude") latitude : String, @Path("longitude") longitude : String) : Call<Qibla>


}