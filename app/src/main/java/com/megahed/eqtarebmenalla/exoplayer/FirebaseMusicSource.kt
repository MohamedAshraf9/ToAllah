package com.megahed.eqtarebmenalla.exoplayer

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.MediaMetadataCompat.*
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import com.megahed.eqtarebmenalla.exoplayer.State.*

class FirebaseMusicSource {

    var songs = emptyList<MediaMetadataCompat>()
    var ayas = emptyList<MediaMetadataCompat>()

    //private val _audiosLiveData = MutableLiveData<List<MediaMetadataCompat>>()
    val audiosLiveData: LiveData<List<Song>> = _audiosLiveData

    val ayasLiveData: LiveData<List<Aya>> = _ayasLiveData

    companion object{
         val _audiosLiveData = MutableLiveData<List<Song>>()
        val _ayasLiveData = MutableLiveData<List<Aya>>()
    }

     fun fetchMediaData() {
        state = STATE_INITIALIZING
        //val allSongs = musicDatabase.getAllSongs()

        //_audiosLiveData.value= null
        audiosLiveData.observeForever{
            state = STATE_INITIALIZING
            songs = it.map { song ->
                MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_ARTIST, song.subtitle)
                    .putString(METADATA_KEY_MEDIA_ID, song.mediaId)
                    .putString(METADATA_KEY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_TITLE, song.title)
                    .putString(METADATA_KEY_DISPLAY_ICON_URI, song.imageUrl)
                    .putString(METADATA_KEY_MEDIA_URI, song.songUrl)
                    .putString(METADATA_KEY_ALBUM_ART_URI, song.imageUrl)
                    .putString(METADATA_KEY_DISPLAY_SUBTITLE, song.subtitle)
                    .putString(METADATA_KEY_DISPLAY_DESCRIPTION, song.subtitle)
                    .build()
            }
            state = STATE_INITIALIZED
            //Log.d("Firebase", "fetchMediaData: ayas: ${ayas[0].description}")
        }
        state = STATE_INITIALIZED

    }

    fun fetchAyaMediaData() {
        state = STATE_INITIALIZING
        //val allSongs = musicDatabase.getAllSongs()

        //_audiosLiveData.value= null
        ayasLiveData.observeForever{
            state = STATE_INITIALIZING
            ayas = it.map { aya ->
                val media = MediaMetadataCompat.Builder()
                    .putString(METADATA_KEY_ARTIST, aya.soraId.toString())
                    .putString(METADATA_KEY_MEDIA_ID, aya.ayaId.toString())
                    .putString(METADATA_KEY_TITLE, aya.text.toString())
                    .putString(METADATA_KEY_DISPLAY_TITLE, aya.text.toString())
                    //.putString(METADATA_KEY_DISPLAY_ICON_URI, aya.imageUrl)
                    .putString(METADATA_KEY_MEDIA_URI, aya.url)
                    //.putString(METADATA_KEY_ALBUM_ART_URI, aya.imageUrl)
                    .putString(METADATA_KEY_DISPLAY_SUBTITLE, "")
                    .putString(METADATA_KEY_DISPLAY_DESCRIPTION, "AYA")
                    .build()

                return@map media
            }

            //Log.d("Firebase", "fetchAyaMediaData: ayas: ${ayas[0].description.description}")

            state = STATE_INITIALIZED
        }
        state = STATE_INITIALIZED

    }

    fun getMetadataForAya(ayaId: Int): MediaMetadataCompat? {
        return ayas.find { it.getString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID) == ayaId.toString() }
    }


    fun asMediaSource(dataSourceFactory: DefaultDataSource.Factory): ConcatenatingMediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()
        songs.forEach { song ->
            val mediaItem: MediaItem =
                MediaItem.fromUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
            val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            concatenatingMediaSource.addMediaSource(mediaSource)
        }
        return concatenatingMediaSource
    }

    fun asAyaMediaSource(dataSourceFactory: DefaultDataSource.Factory, repeatCount: Int): MediaSource {
        val concatenatingMediaSource = ConcatenatingMediaSource()

        ayas.forEach { aya ->
            val mediaItem = MediaItem.fromUri(aya.getString(METADATA_KEY_MEDIA_URI).toUri())
            val progressiveMediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(mediaItem)
            //val loopingMediaSource = LoopingMediaSource(progressiveMediaSource, repeatCount)
            repeat(repeatCount) {
                concatenatingMediaSource.addMediaSource(progressiveMediaSource)
            }
        }

        return concatenatingMediaSource
    }


    fun asMediaItems():  MutableList<MediaBrowserCompat.MediaItem> {
            return songs.map { song ->
                val desc = MediaDescriptionCompat.Builder()
                    .setMediaUri(song.getString(METADATA_KEY_MEDIA_URI).toUri())
                    .setTitle(song.description.title)
                    .setSubtitle(song.description.subtitle)
                    .setMediaId(song.description.mediaId)
                    .setIconUri(song.description.iconUri)
                    .build()
                MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
            }.toMutableList()
    }

    fun asAyaMediaItems():  MutableList<MediaBrowserCompat.MediaItem> {
            return ayas.map { aya ->
                val desc = MediaDescriptionCompat.Builder()
                    .setMediaUri(aya.getString(METADATA_KEY_MEDIA_URI).toUri())
                    .setTitle(aya.description.title)
                    .setSubtitle(aya.description.subtitle)
                    .setMediaId(aya.description.mediaId)
                    .setIconUri(aya.description.iconUri)
                    .build()
                MediaBrowserCompat.MediaItem(desc, FLAG_PLAYABLE)
            }.toMutableList()
    }

    private val onReadyListeners = mutableListOf<(Boolean) -> Unit>()

    private var state: State = STATE_CREATED
        set(value) {
            if(value == STATE_INITIALIZED || value == STATE_ERROR) {
                synchronized(onReadyListeners) {
                    field = value
                    onReadyListeners.forEach { listener ->
                        listener(state == STATE_INITIALIZED)
                    }
                }
            } else {
                field = value
            }
        }

    fun whenReady(action: (Boolean) -> Unit): Boolean {
        if(state == STATE_CREATED || state == STATE_INITIALIZING) {
            onReadyListeners += action
            return false
        } else {
            action(state == STATE_INITIALIZED)
            return true
        }
    }
}

enum class State {
    STATE_CREATED,
    STATE_INITIALIZING,
    STATE_INITIALIZED,
    STATE_ERROR
}















