package com.megahed.eqtarebmenalla.exoplayer.callbacks

import android.app.Notification
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import com.megahed.eqtarebmenalla.common.Constants.NOTIFICATION_ID
import com.megahed.eqtarebmenalla.exoplayer.MusicService
class MusicPlayerNotificationListener(
    private val musicService: MusicService
) : PlayerNotificationManager.NotificationListener {

    val sharedPreferences = musicService.getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

    override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
        super.onNotificationCancelled(notificationId, dismissedByUser)
        if (!sharedPreferences.getBoolean("isPlayingSora", true)) {
            val currentRepeat = sharedPreferences.getInt("all_repeat_counter", 1)
            val totalRepeats = sharedPreferences.getInt("all_repeat", 1)

            if (currentRepeat >= totalRepeats) {
                musicService.apply {
                    stopForeground(true)
                    isForegroundService = false
                    stopSelf()
                }

                sharedPreferences.edit()
                    .remove("all_repeat_counter")
                    .remove("all_repeat")
                    .apply()
            }
        }else {
            musicService.apply {
                stopForeground(true)
                isForegroundService = false
                stopSelf()
            }
        }
    }

    override fun onNotificationPosted(
        notificationId: Int,
        notification: Notification,
        ongoing: Boolean
    ) {
        super.onNotificationPosted(notificationId, notification, ongoing)
        musicService.apply {
            if(ongoing && !isForegroundService) {
                ContextCompat.startForegroundService(
                    this,
                    Intent(applicationContext, this::class.java)
                )
                startForeground(NOTIFICATION_ID, notification)
                isForegroundService = true
            }
        }
    }


}











