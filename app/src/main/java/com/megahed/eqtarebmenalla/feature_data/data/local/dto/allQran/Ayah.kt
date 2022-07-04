package com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran

import com.megahed.eqtarebmenalla.db.model.Aya

data class Ayah(
    val hizbQuarter: Int,
    val juz: Int,
    val manzil: Int,
    val number: Int,
    val numberInSurah: Int,
    val page: Int,
    val ruku: Int,
    val sajda: Boolean,
    val text: String
)
fun Ayah.toAya(soraId:Int): Aya {
    return Aya(
        ayaId =number,
        hizbQuarter=hizbQuarter,
        juz=juz,
        manzil=manzil,
        numberInSurah=numberInSurah,
        page=page,
        ruku=ruku,
        sajda=sajda,
        text=text,
        soraId=soraId


    )
}