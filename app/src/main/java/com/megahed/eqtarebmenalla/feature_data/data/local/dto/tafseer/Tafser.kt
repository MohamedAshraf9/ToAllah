package com.megahed.eqtarebmenalla.feature_data.data.local.dto.tafseer

import com.google.errorprone.annotations.Keep

@Keep
data class Tafser(
    val count: Int,
    val index: Int,
    val verse: Verse

)