package com.megahed.eqtarebmenalla.feature_data.data.tafser

import com.google.errorprone.annotations.Keep

@Keep
data class TafserItem(
    val aya: String,
    val number: String,
    val text: String
)