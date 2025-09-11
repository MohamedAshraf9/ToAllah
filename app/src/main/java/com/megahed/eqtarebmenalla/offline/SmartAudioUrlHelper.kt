package com.megahed.eqtarebmenalla.offline

import com.megahed.eqtarebmenalla.common.Constants
import java.util.Locale

object SmartAudioUrlHelper {

    suspend fun getAudioUrl(
        offlineAudioManager: OfflineAudioManager,
        readerId: String,
        surahId: Int,
        verseId: Int? = null,
        onlineBaseUrl: String,
    ): String {
        val normalizedReaderId = normalizeToAsciiDigits(readerId)
        val offlineUrl = offlineAudioManager.getOfflineAudioUrl(normalizedReaderId, surahId, verseId)
        if (offlineUrl != null) {
            return offlineUrl
        }

        val onlineUrl = if (verseId != null) {
            val cleanBaseUrl = normalizeUrl(onlineBaseUrl).trimEnd('/')
            val surahFormatted = String.format(Locale.US, "%03d", surahId)
            val verseFormatted = String.format(Locale.US, "%03d", verseId)
            "$cleanBaseUrl/${surahFormatted}${verseFormatted}.mp3"
        } else {
            Constants.getSoraLink(normalizeUrl(onlineBaseUrl), surahId)
        }

        return onlineUrl
    }

    private fun normalizeToAsciiDigits(input: String): String {
        return input.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }

    private fun normalizeUrl(url: String): String {
        return url.replace(Regex("[٠-٩]")) { matchResult ->
            when (matchResult.value) {
                "٠" -> "0"
                "١" -> "1"
                "٢" -> "2"
                "٣" -> "3"
                "٤" -> "4"
                "٥" -> "5"
                "٦" -> "6"
                "٧" -> "7"
                "٨" -> "8"
                "٩" -> "9"
                else -> matchResult.value
            }
        }
    }
}