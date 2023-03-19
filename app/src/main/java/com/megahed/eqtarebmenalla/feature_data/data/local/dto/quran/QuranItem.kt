package com.megahed.eqtarebmenalla.feature_data.data.local.dto.quran

import com.google.errorprone.annotations.Keep

@Keep
data class QuranItem(
    val count: Int,
    val index: String,
    val juz: List<Juz>,
    val pages: String,
    val place: String,
    val title: String,
    val titleAr: String,
    val type: String
)