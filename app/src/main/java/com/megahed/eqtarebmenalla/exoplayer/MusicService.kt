package com.megahed.eqtarebmenalla.exoplayer

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.os.SystemClock
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.megahed.eqtarebmenalla.common.Constants.MEDIA_ROOT_ID
import com.megahed.eqtarebmenalla.common.Constants.NETWORK_ERROR
import com.megahed.eqtarebmenalla.common.Constants.NOTIFICATION_ID
import com.megahed.eqtarebmenalla.exoplayer.callbacks.MusicPlaybackPreparer
import com.megahed.eqtarebmenalla.exoplayer.callbacks.MusicPlayerEventListener
import com.megahed.eqtarebmenalla.exoplayer.callbacks.MusicPlayerNotificationListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import javax.inject.Inject

private const val SERVICE_TAG = "MusicService"

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSource.Factory

    @Inject
    lateinit var exoPlayer: ExoPlayer


    lateinit var firebaseMusicSource: FirebaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector


    var isForegroundService = false

    private var curPlayingSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        var curSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(SERVICE_TAG, "onCreate: ***")

        sharedPreferences = getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)

        firebaseMusicSource= FirebaseMusicSource()

        serviceScope.launch {
            firebaseMusicSource.fetchMediaData()
            firebaseMusicSource.fetchAyaMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this,
            mediaSession.sessionToken,
            MusicPlayerNotificationListener(this)
        ) {
            curSongDuration = exoPlayer.duration
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(sharedPreferences, firebaseMusicSource) {
            if (sharedPreferences.getBoolean("isPlayingSora", true)) { curPlayingSong = it }
            preparePlayer(
                firebaseMusicSource.songs,
                firebaseMusicSource.ayas,
                it,
                true
            )
        }

        //sessionToken = mediaSession.sessionToken

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setPlaybackPreparer(musicPlaybackPreparer)
        mediaSessionConnector.setQueueNavigator(MusicQueueNavigator())
        mediaSessionConnector.setPlayer(exoPlayer)

        musicPlayerEventListener = MusicPlayerEventListener(this,musicNotificationManager,exoPlayer)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)







    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {

            //val actualIndex = player.currentMediaItemIndex

            if (sharedPreferences.getBoolean("isPlayingSora", true)) {
                return firebaseMusicSource.songs[windowIndex].description
            }else {
                val repeatCount = sharedPreferences.getInt("repeat_count", 1)
                val actualIndex = windowIndex / repeatCount // Calculate the actual Aya index
                val mediaList = firebaseMusicSource.ayas

                // Ensure actualIndex is within bounds
                return if (actualIndex in mediaList.indices) {
                    mediaList[actualIndex].description
                } else {
                    MediaDescriptionCompat.Builder().setTitle("Unknown Aya").build()
                }
            }
        }
    }


    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        ayas: List<MediaMetadataCompat>?,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        clearPlayer()

        if (sharedPreferences.getBoolean("isPlayingSora", true)) {
            var curSongIndex = 0
            curSongIndex = if (curPlayingSong == null) 0 else songs.indexOf(itemToPlay)
            //exoPlayer.prepare(firebaseMusicSource.asMediaSource(dataSourceFactory))
            val mediaSource = firebaseMusicSource.asMediaSource(dataSourceFactory)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.seekTo(curSongIndex, 0L)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playNow
        }else {
            Log.d(SERVICE_TAG, "preparePlayer: is this??")
            val repeatCount = sharedPreferences.getInt("repeat_count", 1)
            val mediaSource = firebaseMusicSource.asAyaMediaSource(dataSourceFactory, repeatCount)
            exoPlayer.setMediaSource(mediaSource)
            exoPlayer.prepare()
            exoPlayer.playWhenReady = playNow
        }
    }

    private fun clearPlayer() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()  // Clears any previously loaded media items
        exoPlayer.playWhenReady = false  // Reset play state
        //firebaseMusicSource.clearAyas()
        //curPlayingSong = null  // Reset current playing item
    }


    override fun onTaskRemoved(rootIntent: Intent?) {
        // Restart the service if the task is removed
        val restartServiceIntent = Intent(applicationContext, this.javaClass)
        restartServiceIntent.setPackage(packageName)

        val restartServicePendingIntent = PendingIntent.getService(
            applicationContext, 1, restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val alarmService = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmService[AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 1000] =
            restartServicePendingIntent

        //exoPlayer.clearMediaItems()

        super.onTaskRemoved(rootIntent)

        // Remove exoPlayer.stop() to prevent stopping playback when app is cleared
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()

        exoPlayer.removeListener(musicPlayerEventListener)
        exoPlayer.release()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when(parentId) {
            MEDIA_ROOT_ID -> {
                val resultsSent = firebaseMusicSource.whenReady { isInitialized ->
                    if(isInitialized) {
                        if (sharedPreferences.getBoolean("isPlayingSora", true)) {
                        result.sendResult(firebaseMusicSource.asMediaItems())
                        }else {
                            result.sendResult(firebaseMusicSource.asAyaMediaItems())
                        }

                        if(!isPlayerInitialized && (firebaseMusicSource.songs.isNotEmpty() || firebaseMusicSource.ayas.isNotEmpty())) {
                            preparePlayer(
                                firebaseMusicSource.songs,
                                firebaseMusicSource.ayas,
                                firebaseMusicSource.songs[0],
                                false
                            )
                            isPlayerInitialized = true
                        }
                    } else {
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                if(!resultsSent) {
                    result.detach()
                }
                Log.d("MusicService", "onLoadChildren: MEDIA_ROOT_ID")
            }
            else -> {
                Log.d("MusicService", "onLoadChildren: Not MEDIA_ROOT_ID")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("MusicService", "onStartCommand: service is starting?!")

        return START_STICKY // This ensures the service is restarted if killed
       }

    fun updateMetadataForCurrentAya(ayaId: Int) {
        // Get metadata for the current Aya from firebaseMusicSource
        val metadata = firebaseMusicSource.getMetadataForAya(ayaId)
        if (metadata != null) {
            mediaSession.setMetadata(metadata)

            // Update playback state to refresh the notification
            val playbackState = PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PLAYING, exoPlayer.currentPosition, 1f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY_PAUSE or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
                .build()
            mediaSession.setPlaybackState(playbackState)
        }
    }

}























