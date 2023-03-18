package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity

data class RefCountryCode(
    val alpha2: String,
    val alpha3: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val numeric: Int
)