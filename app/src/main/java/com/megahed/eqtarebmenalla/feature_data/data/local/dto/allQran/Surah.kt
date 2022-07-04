package com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran

data class Surah(
    val ayahs: List<Ayah>,
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val number: Int,
    val revelationType: String
)