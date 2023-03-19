package com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.db.model.Sora
@Keep
data class Surah(
    val ayahs: List<Ayah>,
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val number: Int,
    val revelationType: String
)
fun Surah.toSora(ayatNumbers:Int):Sora{
    return Sora(
        soraId = number,
        englishName=englishName,
        englishNameTranslation=englishNameTranslation,
        name=name,
        revelationType=revelationType,
        ayatNumbers =ayatNumbers
    )
}