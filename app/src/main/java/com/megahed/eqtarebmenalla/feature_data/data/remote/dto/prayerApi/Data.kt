package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Data(
    val date: Date,
    val meta: Meta,
    val timings: Timings
)