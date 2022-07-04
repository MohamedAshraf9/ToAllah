package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

data class Date(
    val gregorian: Gregorian,
    val hijri: Hijri,
    val readable: String="",
    val timestamp: String=""
)