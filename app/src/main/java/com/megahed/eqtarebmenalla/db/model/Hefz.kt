package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.megahed.eqtarebmenalla.enm.HefzType

@Entity
data class Hefz(
    var startAyaId:Int,
    var endAyaId:Int,
    var isDone:Boolean=false,
    var hefzType: HefzType


){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
