package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity

import com.google.errorprone.annotations.Keep

@Keep
data class Data(
    val direction: Double,
    val latitude: Double,
    val longitude: Double
)