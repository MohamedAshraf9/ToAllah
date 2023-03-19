package com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse

import com.google.errorprone.annotations.Keep

@Keep
data class RecitersVerse(
    val audio_url_bit_rate_128: String,
    val audio_url_bit_rate_32_: String,
    val audio_url_bit_rate_64: String,
    val id: String,
    val musshaf_type: String,
    val name: String,
    val rewaya: String
)