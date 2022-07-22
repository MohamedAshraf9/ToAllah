package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = QuranListenerReader::class,
            parentColumns = ["id"],
            childColumns = ["readerId"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ]
)
data class SoraSong(
    val SoraId:Int,
    var readerId:String,
    var url:String,
    var isVaForte:Boolean=false
){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
