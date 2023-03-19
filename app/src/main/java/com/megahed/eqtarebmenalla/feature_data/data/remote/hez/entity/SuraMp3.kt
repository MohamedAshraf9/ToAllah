package com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity

import com.google.errorprone.annotations.Keep

@Keep
data class SuraMp3(
    val code: Int,
    val data: Data,
    val status: String
)