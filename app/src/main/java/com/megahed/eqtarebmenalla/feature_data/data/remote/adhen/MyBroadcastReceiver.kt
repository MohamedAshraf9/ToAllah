package com.megahed.eqtarebmenalla.feature_data.data.remote.adhen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.*
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi

import androidx.core.app.NotificationCompat
import com.megahed.eqtarebmenalla.MainActivity
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.feature_data.presentation.ui.home.AdhenAlarmActivity
import java.util.*


class MyBroadcastReceiver : BroadcastReceiver() {
//    var mp: MediaPlayer? = null // Here
//    lateinit var  sharedPreference : SharedPreferences
    @RequiresApi(api = Build.VERSION_CODES.Q)
    override fun onReceive(p0: Context?, p1: Intent?) {


//            val mp = MediaPlayer.create(p0 , R.raw.adhen)
//            mp.start()

        var salet = checkSalaet()


        val intent = Intent(p0, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }




        val pendingIntent: PendingIntent = PendingIntent.getActivity(p0, 0, intent, PendingIntent.FLAG_IMMUTABLE)


        var builder = NotificationCompat.Builder(p0!!, salet.channelId)
            .setSmallIcon(R.drawable.prayer_icon)
            .setContentTitle(salet.title)
            .setContentText(salet.text)
            .setDefaults(NotificationCompat.DEFAULT_VIBRATE)

            .setStyle(
                NotificationCompat.BigTextStyle()

              //  .bigText(salet.text)
            )
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        val notificationManager =
            p0?.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?



// === Removed some obsoletes

// === Removed some obsoletes
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = salet.channelId
            val channel = NotificationChannel(
                channelId,
                "Channel human readable title",
                NotificationManager.IMPORTANCE_HIGH
            )
            val attributes: AudioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val sound: Uri =
                Uri.parse((ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + p0
                    .packageName).toString() + "/" + R.raw.adhen) //Here is FILE_NAME is the name of file that you want to play


            channel.setSound(sound, attributes)
            channel.enableLights( true)
            notificationManager?.createNotificationChannel(channel)
            if (channelId != null) {
                builder.setChannelId(channelId)
            }
        }

        notificationManager?.notify(salet.id, builder.build());


//
//            var i = Intent(p0, AdhenAlarmActivity::class.java)
//        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//
//            p0?.startActivity(i)

    }

    private fun checkSalaet() : Salet{

        var salet : Salet? = null
        val cal: Calendar = Calendar.getInstance()
         var hours = cal.get(Calendar.HOUR_OF_DAY)
         var minute = cal.get(Calendar.MINUTE)

        if (hours in 3..6 ){
            salet = Salet("اذان الفجر",Constants.maw3idha.get((0..10).random()),"fajr",10001)
        }
        else if (hours in 12..13){
            salet = Salet("اذان صلاة الظهر",Constants.maw3idha.get((0..10).random()),"dhuhr",10002)

        }
        else if (hours in 14..16){
            salet = Salet("اذان صلاة العصر",Constants.maw3idha.get((0..10).random()),"asr",10003)

        }
        else if (hours in 17..19){
            salet = Salet("اذان صلاة المغرب",Constants.maw3idha.get((0..10).random()),"maghrib",10004)

        }
        else if (hours in 20..21){
            salet = Salet("اذان صلاة العشاء",Constants.maw3idha.get((0..10).random()),"isha",10005)

        }
        else{
            salet = Salet("اذان الصلاة",Constants.maw3idha.get((0..10).random()),"else",10006)
        }
        return salet!!
    }
}

data class Salet(var title : String,
                    var text: String,
                        var channelId : String,
                            var id : Int)