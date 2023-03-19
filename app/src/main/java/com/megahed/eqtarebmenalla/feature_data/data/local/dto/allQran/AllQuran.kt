package com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran

import com.google.errorprone.annotations.Keep

@Keep
data class AllQuran(
    val surahs: List<Surah>
)