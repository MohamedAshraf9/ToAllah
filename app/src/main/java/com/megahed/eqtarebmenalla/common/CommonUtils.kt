package com.megahed.eqtarebmenalla.common

import android.content.Context
import android.widget.Toast
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

object CommonUtils {

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
        val timeLeftFormatted: String = String.format(
                Locale.getDefault(),
                "%02d:%02d:%02d", hours, minutes, seconds
            )

        return timeLeftFormatted
    }

    fun convertSora(soraId:String,aya:String):String{
        val n=soraId.toInt()
        val ayaNumber=aya.toInt()
        var sec1=""
        var sec2=""
        sec1 = if (n<10){
            "00$n"
        } else if (n in 10..99){
            "0$n"
        } else{
            soraId
        }

        sec2 = if (ayaNumber<10){
            "00$ayaNumber"
        } else if (ayaNumber in 10..99){
            "0$ayaNumber"
        } else{
            aya
        }
        return sec1+sec2
    }

    fun convertSoraPart(soraId:Int,ayaNumber:Int):String{

        return ""
    }

    fun showMessage(context:Context,message:String){
        Toast.makeText(context,message,Toast.LENGTH_LONG).show()
    }

    fun getDay(date: Date): String {
        return DateFormat.getDateInstance(DateFormat.FULL).format(date.time)
    }

    fun getMonth(date: Date): String {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar[Calendar.MONTH]
        val categories: Array<String> =
            App.getInstance().resources.getStringArray(R.array.Month)
        return categories[calendar[Calendar.MONTH]] + " " + calendar[Calendar.YEAR]
    }

}