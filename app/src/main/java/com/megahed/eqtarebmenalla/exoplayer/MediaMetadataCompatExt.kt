package com.megahed.eqtarebmenalla.exoplayer

import android.support.v4.media.MediaMetadataCompat
import com.megahed.eqtarebmenalla.feature_data.data.local.entity.Song

fun MediaMetadataCompat.toSong(): Song? {
    return description?.let {
        Song(
            it.mediaId ?: "",
            it.title.toString(),
            it.subtitle.toString(),
            it.mediaUri.toString(),
            it.iconUri.toString()
        )
    }
}