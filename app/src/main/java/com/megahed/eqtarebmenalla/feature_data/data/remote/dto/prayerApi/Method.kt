package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Method(
    val id: Int=0,
    val location: Location,
    val name: String="",
    val params: Params
)