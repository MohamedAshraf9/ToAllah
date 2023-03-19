package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.quranApi

import com.google.errorprone.annotations.Keep

@Keep
data class QuranDto(
    val reciters: List<Reciter>
)