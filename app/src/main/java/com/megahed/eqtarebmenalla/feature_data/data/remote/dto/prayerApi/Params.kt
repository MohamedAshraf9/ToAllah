package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Params(
    val Fajr: Double=0.0,
    val Isha: String=""
)