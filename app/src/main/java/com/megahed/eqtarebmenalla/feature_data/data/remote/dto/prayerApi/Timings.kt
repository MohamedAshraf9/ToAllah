package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Timings(
    val Asr: String="",
    val Dhuhr: String="",
    val Fajr: String="",
    val Imsak: String="",
    val Isha: String="",
    val Maghrib: String="",
    val Midnight: String="",
    val Sunrise: String="",
    val Sunset: String=""
)