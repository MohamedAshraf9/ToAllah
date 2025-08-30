package com.megahed.eqtarebmenalla.feature_data.presentation.ui.hefz

import android.support.v4.media.MediaBrowserCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.megahed.eqtarebmenalla.common.Constants.MEDIA_ROOT_ID
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.exoplayer.MusicServiceConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HefzRepeatViewModel @Inject constructor(
    private val musicServiceConnection: MusicServiceConnection
) : ViewModel() {

    val isConnected = musicServiceConnection.isConnected
    val playbackState = musicServiceConnection.playbackState

    private val _isMemorizationStopped = MutableLiveData<Boolean>().apply { value = false }
    val isMemorizationStopped: MutableLiveData<Boolean> = _isMemorizationStopped

    private val _currentAyaPosition = MutableLiveData<Int>().apply { value = 0 }
    val currentAyaPosition: MutableLiveData<Int> = _currentAyaPosition

    fun playOrToggleAya(aya: Aya) {
        if (_isMemorizationStopped.value == false) {
            musicServiceConnection.transportControls.playFromMediaId(aya.ayaId.toString(), null)
        }
    }

    fun stopPlayback() {
        musicServiceConnection.transportControls.stop()
    }

    fun pausePlayback() {
        musicServiceConnection.transportControls.pause()
    }

    fun resumePlayback() {
        musicServiceConnection.transportControls.play()
    }

    fun skipToNextAya() {
        musicServiceConnection.transportControls.skipToNext()
    }

    fun stopMemorization() {
        _isMemorizationStopped.value = true
        stopPlayback()
    }

    override fun onCleared() {
        super.onCleared()
        stopMemorization()
        musicServiceConnection.unsubscribe(
            MEDIA_ROOT_ID,
            object : MediaBrowserCompat.SubscriptionCallback() {}
        )
    }
}
