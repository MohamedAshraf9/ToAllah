package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Location(
    val latitude: Double=0.0,
    val longitude: Double=0.0
)