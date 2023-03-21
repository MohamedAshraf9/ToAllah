package com.megahed.eqtarebmenalla.feature_data.data.quranImage

import com.google.errorprone.annotations.Keep

@Keep
data class QuranImageItem(
    val ayah: Int?=null,
    val end_time: Int?=null,
    val page: String?=null,
    val polygon: String?=null,
    val start_time: Int?=null,
    val x: String?=null,
    val y: String?=null
)