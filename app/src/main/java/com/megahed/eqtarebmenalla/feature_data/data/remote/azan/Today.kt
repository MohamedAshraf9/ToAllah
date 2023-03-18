package com.megahed.eqtarebmenalla.feature_data.data.remote.azan

import com.google.gson.annotations.SerializedName

data class Today(
    val Asr: String="",
    val Dhuhr: String="",
    val Fajr: String="",
    @SerializedName("Isha'a")
    val isha : String="",
    val Maghrib: String="",
    val Sunrise: String="",
)