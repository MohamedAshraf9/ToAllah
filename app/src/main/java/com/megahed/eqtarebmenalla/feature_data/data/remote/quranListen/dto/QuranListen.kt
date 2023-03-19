package com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto

import com.google.errorprone.annotations.Keep

@Keep
data class QuranListen(
    val reciters: List<Reciter>
)