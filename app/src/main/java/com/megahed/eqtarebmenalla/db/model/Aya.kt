package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Aya(
    var soraId:Int,
    var ayaText:String
){

    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
