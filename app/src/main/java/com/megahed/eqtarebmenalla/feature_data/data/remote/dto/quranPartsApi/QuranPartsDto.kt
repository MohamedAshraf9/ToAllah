package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.quranPartsApi

import com.google.errorprone.annotations.Keep

@Keep
data class QuranPartsDto(
    val reciters_verse: List<RecitersVerse>
)