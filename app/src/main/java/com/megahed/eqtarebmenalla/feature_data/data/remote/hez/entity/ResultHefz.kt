package com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity

import com.google.errorprone.annotations.Keep

@Keep
data class ResultHefz(
    val code: Int,
    val data: List<Reway>,
    val status: String
)