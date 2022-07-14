package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
        entity = AzkarCategory::class,
        parentColumns = ["id"],
        childColumns = ["catId"],
        onDelete = ForeignKey.CASCADE,
        onUpdate = ForeignKey.CASCADE
    )
    ]
)
data class ElZekr(
    var count:String,
    var description:String,
    var reference:String,
    var zekr:String,
    var isVaForte:Boolean=false,
    var catId:Int

){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
