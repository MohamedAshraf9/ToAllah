package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity

import com.google.errorprone.annotations.Keep

@Keep
data class Qibla(
    val code: Int,
    val data: Data,
    val status: String
)