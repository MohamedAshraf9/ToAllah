package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Entity
@Keep
data class Sora(
    @PrimaryKey
    val soraId: Int,//number
    val englishName: String,
    val englishNameTranslation: String,
    val name: String,
    val revelationType: String,
    val ayatNumbers: Int

){

}
