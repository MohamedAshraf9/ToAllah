package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity
data class TasbehData(
    var time:Date,
    var tasbehId:Int,
    var target:Int
){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
