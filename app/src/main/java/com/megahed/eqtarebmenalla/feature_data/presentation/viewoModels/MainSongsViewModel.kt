package com.megahed.eqtarebmenalla.feature_data.presentation.viewoModels

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Constants.MEDIA_ROOT_ID
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.exoplayer.MusicServiceConnection
import com.megahed.eqtarebmenalla.exoplayer.currentPlaybackPosition
import com.megahed.eqtarebmenalla.exoplayer.isPlayEnabled
import com.megahed.eqtarebmenalla.exoplayer.isPlaying
import com.megahed.eqtarebmenalla.exoplayer.isPrepared
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainSongsViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {
    private val _mediaItems = MutableLiveData<Resource<List<Song>>>()
    val mediaItems: LiveData<Resource<List<Song>>> = _mediaItems

    val isConnected = musicServiceConnection.isConnected
    val networkError = musicServiceConnection.networkError
    val curPlayingSong = musicServiceConnection.curPlayingSong
    val playbackState = musicServiceConnection.playbackState

    private var currentPosition: Long = 0L

    init {

       /* _mediaItems.postValue(Resource.Loading(null))
        musicServiceConnection.subscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {
            override fun onChildrenLoaded(
                parentId: String,
                children: MutableList<MediaBrowserCompat.MediaItem>
            ) {
                super.onChildrenLoaded(parentId, children)
                val items = children.map {
                    Song(
                        it.mediaId!!,
                        it.description.title.toString(),
                        it.description.subtitle.toString(),
                        it.description.mediaUri.toString(),
                        it.description.iconUri.toString()
                    )
                }
                _mediaItems.postValue(Resource.Success(items))
            }
        })*/
    }






//    private fun playPauseOrToggle(mediaItem: String, newAudioList: List<Song>?) {
//        if (mediaItem.isEmpty()) return
//        val playState = playbackState.value
//        val isPrepared = playbackState.value?.isPrepared ?: false
//        val currentSongId = curPlayingSong.value?.id
//
//        if (isPrepared && mediaItem == currentSongId) {
//            // If we call this fun with the same current playing song
//            // We can pause it if playbackState.isPlaying
//            // We can play it again from start if playbackState.isPlayEnabled
//            playState?.let { playbackState ->
//                when {
//                    playbackState.isPlaying -> musicServiceConnection.transportControls.pause()
//                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
//                    else -> Unit
//                }
//            }
//        } else {
//            // New song so play it
//            playNewAudio(newAudioList, mediaItem)
//        }
//    }

    private fun playNewAudio(newAudioList: List<Song>?, mediaItem: String) {
        if (newAudioList != null) {
            newAudioChosen(mediaItem)
        } else {
            // if the passed list is null play from all audio list
            newAudioChosen( mediaItem)

        }
    }

    private fun newAudioChosen(mediaItem: String) {
        musicServiceConnection.transportControls.playFromMediaId(mediaItem, null)
    }

    fun playOrPause() {
        val isPlaying = playbackState.value?.state == PlaybackStateCompat.STATE_PLAYING
        if (isPlaying) musicServiceConnection.transportControls.pause()
        else musicServiceConnection.transportControls.play()
    }

    fun stopPlayback() {
        musicServiceConnection.transportControls.stop()
    }


    fun skipToNext() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPrevious() {
        musicServiceConnection.transportControls.skipToPrevious()
    }




//    fun getPlayBackState(): PlaybackStateCompat = playbackState.value ?: EMPTY_PLAYBACK_STATE





    fun skipToNextSong() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun skipToPreviousSong() {
        musicServiceConnection.transportControls.skipToPrevious()
    }

    fun seekTo(pos: Long) {
        musicServiceConnection.transportControls.seekTo(pos)
    }

    fun playOrToggleSong(mediaItem: Song, toggle: Boolean = false) {
        val isPrepared = playbackState.value?.isPrepared ?: false
        if(isPrepared && mediaItem.mediaId ==
            curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID) && curPlayingSong.value?.description?.description != "AYA") {
            playbackState.value?.let { playbackState ->
                when {
                    playbackState.isPlaying -> if(toggle) musicServiceConnection.transportControls.pause()
                    playbackState.isPlayEnabled -> musicServiceConnection.transportControls.play()
                    else -> Unit
                }
            }
        } else {
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
        }
    }

    suspend fun playSoraSegment(
        mediaItem: Song,
        toggle: Boolean = false,
        startTime: Int,
        endTime: Int
    ) {
        val currentMediaId = curPlayingSong.value?.getString(METADATA_KEY_MEDIA_ID)
        val isSameSong = mediaItem.mediaId == currentMediaId

        if (isSameSong) {
            // Seek and play the segment dynamically
            musicServiceConnection.transportControls.seekTo(startTime.toLong())
            musicServiceConnection.transportControls.play()
        } else {
            // Prepare the new media item and play from the desired segment
            musicServiceConnection.transportControls.playFromMediaId(mediaItem.mediaId, null)
            musicServiceConnection.transportControls.seekTo(startTime.toLong())
        }

        // Monitor playback to dynamically stop or move to the next step at `endTime`
        monitorPlaybackSegment(endTime.toLong())
    }

    private var positionCheckJob: Job? = null

    private suspend fun monitorPlaybackSegment(endTime: Long) {
        delay(500)
        positionCheckJob?.cancel() // Cancel any previous monitoring jobs
        positionCheckJob = viewModelScope.launch {
            while (isActive) {
                val playbackPosition = musicServiceConnection.playbackState.value?.currentPlaybackPosition
                if (playbackPosition != null && playbackPosition >= endTime) {
                    musicServiceConnection.transportControls.pause() // Stop playback at `endTime`
                    break // Exit the loop
                }
                delay(100) // Check every 100ms for precise control
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {})
    }
}

















