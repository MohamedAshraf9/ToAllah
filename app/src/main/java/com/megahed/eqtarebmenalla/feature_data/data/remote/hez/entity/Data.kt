package com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity

import com.google.errorprone.annotations.Keep

@Keep
data class Data(
    val ayahs: List<Ayah>,
    val edition: Edition,
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val number: Int,
    val numberOfAyahs: Int,
    val revelationType: String
)