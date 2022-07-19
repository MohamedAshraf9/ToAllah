package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Tasbeh(
    var tasbehName:String,
    var target: Int=100

){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0

    override fun toString(): String {
        return tasbehName
    }
}
