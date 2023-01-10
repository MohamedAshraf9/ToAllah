package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song

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

fun SoraSong.toSong(readerName:String?,image:String?="https://upload.wikimedia.org/wikipedia/commons/thumb/8/89/The_Ka%27ba%2C_Great_Mosque_of_Mecca%2C_Saudi_Arabia_%284%29.jpg/1200px-The_Ka%27ba%2C_Great_Mosque_of_Mecca%2C_Saudi_Arabia_%284%29.jpg"): Song {
    return Song(id.toString(),
        Constants.SORA_OF_QURAN[SoraId],readerName?:"",url,image?:"")
}

