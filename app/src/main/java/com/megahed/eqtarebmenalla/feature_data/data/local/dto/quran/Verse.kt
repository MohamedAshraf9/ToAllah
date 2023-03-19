package com.megahed.eqtarebmenalla.feature_data.data.local.dto.quran

import com.google.errorprone.annotations.Keep

@Keep
data class Verse(
    val end: String,
    val start: String
)