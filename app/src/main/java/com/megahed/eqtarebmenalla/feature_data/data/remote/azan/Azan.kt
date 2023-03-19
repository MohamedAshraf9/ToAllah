package com.megahed.eqtarebmenalla.feature_data.data.remote.azan

import com.google.errorprone.annotations.Keep

@Keep
data class Azan(
    val city: String,
    val date: String,
    val today: Today,
    val tomorrow: Tomorrow
)