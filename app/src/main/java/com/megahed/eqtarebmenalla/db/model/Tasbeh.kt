package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Tasbeh(
    var tasbehName:String

){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0

    override fun toString(): String {
        return tasbehName
    }
}
