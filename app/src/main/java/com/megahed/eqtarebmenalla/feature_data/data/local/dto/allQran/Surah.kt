package com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran

import com.megahed.eqtarebmenalla.db.model.Sora

data class Surah(
    val ayahs: List<Ayah>,
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val number: Int,
    val revelationType: String
)
fun Surah.toSora():Sora{
    return Sora(
        soraId = number,
        englishName=englishName,
        englishNameTranslation=englishNameTranslation,
        name=name,
        revelationType=revelationType
    )
}