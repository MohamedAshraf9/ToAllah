package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class PrayerTime (
    @PrimaryKey
    var id:Int=1,
    var date:String="",
    var Asr: String="",
    var Dhuhr: String="",
    var Fajr: String="",
    var Isha: String="",
    var Maghrib: String="",
    var Sunrise: String=""
    
    
)
/*
{
    @PrimaryKey(autoGenerate = true)
    var id:Int=0
}*/
