package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat.METADATA_KEY_MEDIA_ID
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import com.megahed.eqtarebmenalla.common.Constants.MEDIA_ROOT_ID
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.exoplayer.MusicServiceConnection
import com.megahed.eqtarebmenalla.exoplayer.isPlayEnabled
import com.megahed.eqtarebmenalla.exoplayer.isPlaying
import com.megahed.eqtarebmenalla.exoplayer.isPrepared
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class HefzRepeatViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val isConnected = musicServiceConnection.isConnected
    val playbackState = musicServiceConnection.playbackState
    val currentPlayingSong = musicServiceConnection.curPlayingSong

    fun playOrToggleAya(aya: Aya) {
        musicServiceConnection.transportControls.playFromMediaId(aya.ayaId.toString(), null)
    }



    fun stopPlayback() {
        musicServiceConnection.transportControls.stop()
    }

    override fun onCleared() {
        super.onCleared()
        musicServiceConnection.unsubscribe(MEDIA_ROOT_ID, object : MediaBrowserCompat.SubscriptionCallback() {})
    }

}
