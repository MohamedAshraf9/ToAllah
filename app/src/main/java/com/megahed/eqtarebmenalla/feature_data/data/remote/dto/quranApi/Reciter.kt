package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.quranApi

import com.google.errorprone.annotations.Keep

@Keep
data class Reciter(
    val Server: String,
    val count: String,
    val id: String,
    val letter: String,
    val name: String,
    val rewaya: String,
    val suras: String
)