package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    indices = [Index(name = "cat", value = ["catName"], unique = true)]
)
data class AzkarCategory(
    var catName:String=""
){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
