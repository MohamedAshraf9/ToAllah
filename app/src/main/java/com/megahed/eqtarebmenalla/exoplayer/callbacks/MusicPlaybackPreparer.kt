package com.megahed.eqtarebmenalla.exoplayer.callbacks

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.ResultReceiver
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.megahed.eqtarebmenalla.exoplayer.FirebaseMusicSource

class MusicPlaybackPreparer(
    private val sharedPreferences: SharedPreferences,
    private val firebaseMusicSource: FirebaseMusicSource,
    private val playerPrepared: (MediaMetadataCompat?) -> Unit
) : MediaSessionConnector.PlaybackPreparer {

    override fun onCommand(
        player: Player,
        command: String,
        extras: Bundle?,
        cb: ResultReceiver?
    ): Boolean = false


    override fun onPrepare(playWhenReady: Boolean) = Unit

    override fun onPrepareFromMediaId(mediaId: String, playWhenReady: Boolean, extras: Bundle?) {
        firebaseMusicSource.whenReady {
            var itemToPlay: MediaMetadataCompat? = null
            if (sharedPreferences.getBoolean("isPlayingSora", true)) {
                itemToPlay = firebaseMusicSource.songs.find { mediaId == it.description.mediaId }
                Log.d("MusicPlaybackPreparer", "onPrepareFromMediaId: itemToPlay: ${itemToPlay?.description}")
            }else {
                itemToPlay = firebaseMusicSource.ayas.find { mediaId == it.description.mediaId }
            }

            playerPrepared(itemToPlay)
        }

    }

    override fun onPrepareFromSearch(query: String, playWhenReady: Boolean, extras: Bundle?) = Unit

    override fun onPrepareFromUri(uri: Uri, playWhenReady: Boolean, extras: Bundle?) = Unit


    override fun getSupportedPrepareActions(): Long {
        return PlaybackStateCompat.ACTION_PREPARE_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_PLAY_FROM_MEDIA_ID or
                PlaybackStateCompat.ACTION_SEEK_TO
    }



}















