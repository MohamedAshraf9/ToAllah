package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Sora(
    @PrimaryKey
    val soraId: Int,//number
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val revelationType: String

){

}
