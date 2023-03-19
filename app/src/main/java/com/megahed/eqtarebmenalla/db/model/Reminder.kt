package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.enm.RepeatType
import java.util.*
import kotlin.collections.ArrayList

@Entity
@Keep
data class Reminder(
    var time:Date,
    var duration:RepeatType,
    var spacifecDays:ArrayList<Int>
){
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}
