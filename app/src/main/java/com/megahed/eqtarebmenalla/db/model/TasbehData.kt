package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.*

@Entity(
    foreignKeys = [ForeignKey(
        entity = Tasbeh::class,
        parentColumns = ["id"],
        childColumns = ["tasbehId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )]
)
data class TasbehData(
    var time:Date,
    var target:Int=0,
    var tasbehId:Int
){
    @PrimaryKey(autoGenerate = true)
    var id:Long=0
}
