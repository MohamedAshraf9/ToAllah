package com.megahed.eqtarebmenalla.common

import java.text.SimpleDateFormat
import java.util.*

object Constants {
    const val BASE_URL = "https://api.aladhan.com/"

    fun convertSalahTime(string: String):String{
        var num=string.substring(0,2).toInt()
        if (num>12){ num-=12 }
        else return string
        return if (num<=9)
            "0$num"+string.substring(2,string.length)
        else "$num"+string.substring(2,string.length)


    }


    fun getCurrentTime(): String {
        val c = Calendar.getInstance()
        val sdf = SimpleDateFormat("HH:mm:ss")
       return sdf.format(c.time)
    }

    fun getTimeLong(time:String,isCurrent:Boolean):Long{
        var sdf = SimpleDateFormat("HH:mm")
        if (isCurrent)
             sdf = SimpleDateFormat("HH:mm:ss")
       return sdf.parse(time)?.time ?: 0
    }


     fun updateCountDownText(dd: Long): String {
        val hours = (dd / 1000).toInt() / 3600
        val minutes = (dd / 1000 % 3600).toInt() / 60
        val seconds = (dd / 1000).toInt() % 60
        val timeLeftFormatted: String = if (hours > 0) {
            String.format(
                Locale.getDefault(),
                "%d:%02d:%02d", hours, minutes, seconds
            )
        } else {
            String.format(
                Locale.getDefault(),
                "%02d:%02d", minutes, seconds
            )
        }

        return timeLeftFormatted
    }


}