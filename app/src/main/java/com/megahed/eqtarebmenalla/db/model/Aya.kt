package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.Ayah

@Entity(
    foreignKeys = [ForeignKey(
        entity = Sora::class,
        parentColumns = ["soraId"],
        childColumns = ["soraId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
@Keep
data class Aya(
    @PrimaryKey
    val ayaId: Int,//number
    val hizbQuarter: Int,
    val juz: Int,
    val manzil: Int,
    val numberInSurah: Int,
    val page: Int,
    val ruku: Int,
    val sajda: Boolean,
    val text: String,
    var isVaForte: Boolean=false,
    val soraId:Int,
    var url: String?
)
