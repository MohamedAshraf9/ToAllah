package com.megahed.eqtarebmenalla.offline

import android.util.Log
import com.megahed.eqtarebmenalla.common.Constants

object SmartAudioUrlHelper {

    suspend fun getAudioUrl(
        offlineAudioManager: OfflineAudioManager,
        readerId: String,
        surahId: Int,
        verseId: Int? = null,
        onlineBaseUrl: String,
    ): String {
        val offlineUrl = offlineAudioManager.getOfflineAudioUrl(readerId, surahId, verseId)
        if (offlineUrl != null) {
            return offlineUrl
        }

        val onlineUrl = if (verseId != null) {
            val cleanBaseUrl = onlineBaseUrl.trimEnd('/')
            val surahFormatted = String.format("%03d", surahId)
            val verseFormatted = String.format("%03d", verseId)
            "$cleanBaseUrl/${surahFormatted}${verseFormatted}.mp3"
        } else {
            Constants.getSoraLink(onlineBaseUrl, surahId)
        }

        return onlineUrl
    }
}