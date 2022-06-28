package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class ElZekr(
    var catId:Int,
    var count:Int,
    var description:String,
    var reference:String,
    var zekr:String

){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
