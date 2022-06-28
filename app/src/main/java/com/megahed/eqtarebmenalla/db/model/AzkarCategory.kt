package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class AzkarCategory(
    var catName:String=""
){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
