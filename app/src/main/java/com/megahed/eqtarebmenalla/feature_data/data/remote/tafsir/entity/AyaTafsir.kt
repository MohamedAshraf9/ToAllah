package com.megahed.eqtarebmenalla.feature_data.data.remote.tafsir.entity

import com.google.errorprone.annotations.Keep

@Keep
data class AyaTafsir(
    val ayah_number: Int,
    val ayah_url: String,
    val tafseer_id: Int,
    val tafseer_name: String,
    val text: String
)