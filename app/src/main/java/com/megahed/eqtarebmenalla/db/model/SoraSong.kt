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
    @PrimaryKey
    val id:Int,
    val SoraId:Int,
    val readerId:String,
    val url:String,
    var isVaForte:Boolean=false
)
