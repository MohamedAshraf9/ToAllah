package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Meta(
    val latitude: Double=0.0,
    val latitudeAdjustmentMethod: String="",
    val longitude: Double=0.0,
    val method: Method,
    val midnightMode: String="",
    val offset: Offset,
    val school: String="",
    val timezone: String=""
)