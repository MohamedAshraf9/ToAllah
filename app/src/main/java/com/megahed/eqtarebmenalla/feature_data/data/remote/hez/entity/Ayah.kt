package com.megahed.eqtarebmenalla.feature_data.data.remote.hez.entity

data class Ayah(
    val audio: String,
    val audioSecondary: List<String>,
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