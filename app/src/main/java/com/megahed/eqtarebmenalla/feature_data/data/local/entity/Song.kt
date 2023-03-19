package com.megahed.eqtarebmenalla.feature_data.data.local.entity

import com.google.errorprone.annotations.Keep

@Keep
data class Song(
    val mediaId: String = "",
    val title: String = "",
    val subtitle: String = "",
    val songUrl: String = "",
    val imageUrl: String = ""
)