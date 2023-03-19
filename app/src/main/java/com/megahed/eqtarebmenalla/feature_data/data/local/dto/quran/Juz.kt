package com.megahed.eqtarebmenalla.feature_data.data.local.dto.quran

import com.google.errorprone.annotations.Keep

@Keep
data class Juz(
    val index: String,
    val verse: Verse
)