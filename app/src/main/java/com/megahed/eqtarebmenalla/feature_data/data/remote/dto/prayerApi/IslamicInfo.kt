package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class IslamicInfo(
    val code: Int=0,
    val status: String="",
    val data: Data?=null
)