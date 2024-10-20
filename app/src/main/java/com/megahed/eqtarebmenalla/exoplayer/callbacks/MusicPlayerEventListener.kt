package com.megahed.eqtarebmenalla.exoplayer.callbacks

import android.util.Log
import android.widget.Toast
import com.google.android.exoplayer2.ExoPlaybackException
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.megahed.eqtarebmenalla.exoplayer.MusicNotificationManager
import com.megahed.eqtarebmenalla.exoplayer.MusicService

class MusicPlayerEventListener(
    private val musicService: MusicService,
    private val notificationManager: MusicNotificationManager,
    private val player: ExoPlayer
) : Player.Listener  {


    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(playWhenReady, reason)
        if(reason == Player.STATE_READY && !playWhenReady) {
            musicService.stopForeground(false)
        }
    }




    override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)
        //error.message?.let { Log.e("dsfsfdssf", it) }
        Toast.makeText(musicService, "An unknown error", Toast.LENGTH_LONG).show()
    }

    /*override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
        super.onPlayerStateChanged(playWhenReady, playbackState)
        if(playbackState == Player.STATE_READY && !playWhenReady) {
            musicService.stopForeground(false)
        }
    }*/

    override fun onPlaybackStateChanged(playbackState: Int) {
        super.onPlaybackStateChanged(playbackState)
        when (playbackState) {
            Player.STATE_BUFFERING,
            Player.STATE_READY -> {
                notificationManager.showNotification(player)
                // If playback is paused we remove the foreground state which allows the
                // notification to be dismissed. An alternative would be to provide a "close"
                // button in the notification which stops playback and clears the notification.
                if (playbackState == Player.STATE_READY && !player.playWhenReady) {
                    musicService.stopForeground(false)
                }
            }
            else -> {
                notificationManager.hideNotification()
                if (!player.playWhenReady) {
                    musicService.stopForeground(true)
                }
            }
        }
    }
}