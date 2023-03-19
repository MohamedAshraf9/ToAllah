package com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity

import com.google.errorprone.annotations.Keep

@Keep
data class Reway(
    val direction: String,
    val englishName: String,
    val format: String,
    val identifier: String,
    val language: String,
    val name: String,
    val type: String
)