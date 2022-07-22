package com.megahed.eqtarebmenalla.exoplayer.callbacks

import android.app.Notification
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.exoplayer.MusicService

class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {


    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        musicService.apply {
            stopForeground(true)
            isForegroundService = false
            stopSelf()
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        Log.d("MusicServiceConnection","clicked global  $ongoing")
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            if(ongoing && !isForegroundService) {
                //Log.d("MusicServiceConnection","clicked inner true $ongoing")
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)
                )
                startForeground(Constants.NOTIFICATION_ID, notification)
                isForegroundService = true
            }
            else{
                Log.d("MusicServiceConnection","clicked inner false $ongoing")
            }
        }
    }


}











