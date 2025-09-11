package com.megahed.eqtarebmenalla.offline

import android.app.DownloadManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.dao.DownloadedAudioDao
import com.megahed.eqtarebmenalla.db.dao.OfflineSettingsDao
import com.megahed.eqtarebmenalla.db.model.DownloadType
import com.megahed.eqtarebmenalla.db.model.DownloadedAudio
import com.megahed.eqtarebmenalla.db.model.OfflineSettings
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date
import java.util.Locale

@Singleton
class OfflineAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedAudioDao: DownloadedAudioDao,
    private val offlineSettingsDao: OfflineSettingsDao,
    private val quranListenerReaderRepository: QuranListenerReaderRepository,
) {
    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    suspend fun getOfflineAudioUrl(readerId: String, surahId: Int, verseId: Int? = null): String? {
        return try {

            if (verseId != null) {
                val verseAudio =
                    downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
                if (verseAudio?.isComplete == true && File(verseAudio.localFilePath).exists()) {
                    return verseAudio.localFilePath
                }
            }

            val surahAudio = downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
            if (surahAudio?.isComplete == true && File(surahAudio.localFilePath).exists()) {
                return surahAudio.localFilePath
            }

            null

        } catch (e: Exception) {

            null
        }
    }

    suspend fun downloadVerseAudio(
        readerId: String,
        surahId: Int,
        verseId: Int,
        verseName: String,
        readerName: String,
        audioUrl: String,
    ): Boolean {
        return try {
            // Check network first
            if (!isNetworkAvailable()) {
                throw java.net.UnknownHostException("لا يوجد اتصال بالإنترنت")
            }

            val existing = downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
            if (existing?.isComplete == true && File(existing.localFilePath).exists()) {
                return true
            }

            val audioId = "${readerId}_${surahId}_${verseId}"
            val folderType = "verses"
            val fileName = "${verseName}_${readerName}.mp3"
            val folder = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "offline_audio/$readerName/$folderType"
            )
            if (!folder.exists()) folder.mkdirs()

            val localFile = File(folder, fileName)

            val success = downloadFileWithTimeout(audioUrl, localFile, audioId)

            if (success) {
                val downloadedAudio = DownloadedAudio(
                    id = audioId,
                    readerId = readerId,
                    surahId = surahId,
                    verseId = verseId,
                    surahName = verseName,
                    readerName = readerName,
                    localFilePath = localFile.absolutePath,
                    originalUrl = audioUrl,
                    downloadDate = Date(),
                    fileSize = localFile.length(),
                    isComplete = true,
                    downloadType = DownloadType.INDIVIDUAL_VERSE
                )
                downloadedAudioDao.insertDownloadedAudio(downloadedAudio)
            }

            success
        } catch (e: java.net.UnknownHostException) {
            throw e
        } catch (e: java.net.SocketTimeoutException) {
            throw e
        } catch (e: java.net.ConnectException) {
            throw e
        } catch (e: javax.net.ssl.SSLException) {
            throw e
        } catch (e: java.io.IOException) {
            throw e
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun downloadFileWithTimeout(
        url: String,
        localFile: File,
        audioId: String,
        timeoutMs: Long = 30000L,
    ): Boolean = withContext(Dispatchers.IO) {
        var inputStream: java.io.InputStream? = null
        var outputStream: java.io.FileOutputStream? = null
        var connection: java.net.HttpURLConnection? = null

        try {
            val urlConnection = java.net.URL(url)
            connection = urlConnection.openConnection() as java.net.HttpURLConnection
            connection.connectTimeout = timeoutMs.toInt()
            connection.readTimeout = timeoutMs.toInt()
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode != java.net.HttpURLConnection.HTTP_OK) {
                throw java.io.IOException("خطأ في الخادم: كود الخطأ $responseCode")
            }

            val fileLength = connection.contentLength
            inputStream = connection.inputStream
            outputStream = java.io.FileOutputStream(localFile)

            val buffer = ByteArray(4096)
            var downloaded = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                downloaded += bytesRead

                if (fileLength > 0) {
                    val progress = ((downloaded * 100) / fileLength).toInt().coerceIn(0, 100)
                    withContext(Dispatchers.Main) {
                        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                            put(audioId, progress)
                        }
                    }
                }

                if (!isNetworkAvailable()) {
                    throw java.net.UnknownHostException("انقطع الاتصال بالإنترنت أثناء التحميل")
                }
            }

            outputStream.flush()

            withContext(Dispatchers.Main) {
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(audioId)
                }
            }

            true

        } catch (e: java.net.UnknownHostException) {
            localFile.delete() // Clean up partial file
            throw java.net.UnknownHostException("لا يوجد اتصال بالإنترنت")
        } catch (e: java.net.SocketTimeoutException) {
            localFile.delete()
            throw java.net.SocketTimeoutException("انتهت مهلة الاتصال")
        } catch (e: java.net.ConnectException) {
            localFile.delete()
            throw java.net.ConnectException("فشل في الاتصال بالخادم")
        } catch (e: javax.net.ssl.SSLException) {
            localFile.delete()
            throw javax.net.ssl.SSLException("خطأ في شهادة الأمان")
        } catch (e: java.io.IOException) {
            localFile.delete()
            throw java.io.IOException("خطأ في التحميل أو التخزين")
        } catch (e: Exception) {
            localFile.delete()
            throw Exception("خطأ غير معروف: ${e.message}")
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
                connection?.disconnect()
            } catch (e: Exception) {

            }

            withContext(Dispatchers.Main) {
                _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                    remove(audioId)
                }
            }
        }
    }

    suspend fun isVerseAudioDownloaded(readerId: String, surahId: Int, verseId: Int): Boolean {
        return try {
            val audio = downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
            audio?.isComplete == true && File(audio.localFilePath).exists() && File(audio.localFilePath).length() > 0
        } catch (e: Exception) {
            false
        }
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities =
                    connectivityManager.getNetworkCapabilities(network) ?: return false

                return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                return networkInfo?.isConnectedOrConnecting == true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun downloadSurahAudio(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String,
    ): Boolean {
        return downloadAudio(
            audioId = "${readerId}_${surahId}",
            readerId = readerId,
            surahId = surahId,
            verseId = null,
            audioName = surahName,
            readerName = readerName,
            audioUrl = audioUrl,
            downloadType = DownloadType.FULL_SURAH
        )
    }

    private suspend fun downloadAudio(
        audioId: String,
        readerId: String,
        surahId: Int,
        verseId: Int?,
        audioName: String,
        readerName: String,
        audioUrl: String,
        downloadType: DownloadType,
    ): Boolean {
        return try {

            val existing = if (verseId != null) {
                downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
            } else {
                downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
            }

            if (existing?.isComplete == true && File(existing.localFilePath).exists()) {

                return true
            }

            val folderType = if (verseId != null) "verses" else "surahs"
            val fileName = "${audioName}_${readerName}.mp3"
            val folder = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "offline_audio/$readerName/$folderType"
            )
            if (!folder.exists()) folder.mkdirs()

            val localFile = File(folder, fileName)

            val request = DownloadManager.Request(Uri.parse(audioUrl))
                .setDestinationUri(Uri.fromFile(localFile))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("تحميل $audioName")
                .setDescription("$readerName - $audioName")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            val downloadedAudio = DownloadedAudio(
                id = audioId,
                readerId = readerId,
                surahId = surahId,
                verseId = verseId,
                surahName = audioName,
                readerName = readerName,
                localFilePath = localFile.absolutePath,
                originalUrl = audioUrl,
                downloadDate = Date(),
                fileSize = 0L,
                isComplete = false,
                downloadType = downloadType
            )

            downloadedAudioDao.insertDownloadedAudio(downloadedAudio)
            monitorDownload(downloadId, audioId, localFile)

            true
        } catch (e: Exception) {
            false
        }
    }

    private fun monitorDownload(downloadId: Long, audioId: String, localFile: File) {
        CoroutineScope(Dispatchers.IO).launch {
            var isComplete = false
            while (!isComplete) {
                val cursor =
                    downloadManager.query(DownloadManager.Query().setFilterById(downloadId))
                if (cursor != null && cursor.moveToFirst()) {
                    val status =
                        cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                    val downloaded =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                    val total =
                        cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                    if (total > 0L) {
                        val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)
                        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                            put(audioId, progress)
                        }
                    }

                    when (status) {
                        DownloadManager.STATUS_SUCCESSFUL -> {

                            val parts = audioId.split("_")
                            if (parts.size >= 2) {
                                val readerId = parts[0]
                                val surahId = parts[1].toIntOrNull()
                                val verseId = if (parts.size >= 3) parts[2].toIntOrNull() else null

                                if (surahId != null) {
                                    val existingAudio = if (verseId != null) {
                                        downloadedAudioDao.getDownloadedVerseAudio(
                                            readerId,
                                            surahId,
                                            verseId
                                        )
                                    } else {
                                        downloadedAudioDao.getDownloadedSurahAudio(
                                            readerId,
                                            surahId
                                        )
                                    }

                                    existingAudio?.let {
                                        downloadedAudioDao.insertDownloadedAudio(
                                            it.copy(
                                                isComplete = true,
                                                fileSize = localFile.length()
                                            )
                                        )
                                    }
                                }
                            }

                            isComplete = true
                            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                                remove(audioId)
                            }
                        }

                        DownloadManager.STATUS_FAILED -> {
                            isComplete = true
                            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                                remove(audioId)
                            }
                        }
                    }
                }
                cursor?.close()
                if (!isComplete) delay(1000)
            }
        }
    }

    suspend fun getOfflineSettings(): OfflineSettings {
        return offlineSettingsDao.getOfflineSettings() ?: OfflineSettings()
    }

    suspend fun updateOfflineSettings(settings: OfflineSettings) {
        offlineSettingsDao.insertOfflineSettings(settings)
    }

    suspend fun areVersesDownloaded(
        readerId: String,
        surahId: Int,
        startVerse: Int,
        endVerse: Int,
    ): Boolean {
        for (verseId in startVerse..endVerse) {
            val audio = getDownloadedVerseAudio(readerId, surahId, verseId)
            if (audio?.isComplete != true || !File(audio.localFilePath).exists()) {
                return false
            }
        }
        return true
    }


    suspend fun getVerseAudioPath(readerId: String, surahId: Int, verseId: Int): String? {
        val downloaded = getDownloadedVerseAudio(readerId, surahId, verseId)
        return if (downloaded?.isComplete == true && File(downloaded.localFilePath).exists()) {
            downloaded.localFilePath
        } else {
            null
        }
    }

    private suspend fun getDownloadedVerseAudio(
        readerId: String,
        surahId: Int,
        verseId: Int,
    ): DownloadedAudio? {
        return downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
    }

    suspend fun getMissingVerses(
        readerId: String,
        surahId: Int,
        startVerse: Int,
        endVerse: Int,
    ): List<Int> {
        val missing = mutableListOf<Int>()
        for (verseId in startVerse..endVerse) {
            val audio = getDownloadedVerseAudio(readerId, surahId, verseId)
            if (audio?.isComplete != true || !File(audio.localFilePath).exists()) {
                missing.add(verseId)
            }
        }
        return missing
    }

    suspend fun getDownloadedSurahAudio(readerId: String, surahId: Int): DownloadedAudio? {
        return downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
    }

    suspend fun isSurahDownloaded(readerId: String, surahId: Int): Boolean {
        val downloaded = getDownloadedSurahAudio(readerId, surahId)
        return downloaded?.isComplete == true && File(downloaded.localFilePath).exists()
    }

    private suspend fun constructAudioUrl(readerId: String, surahId: Int): String {
        return try {
            val normalizedReaderId = normalizeToAsciiDigits(readerId)
            val reader =
                quranListenerReaderRepository.getQuranListenerReaderById(normalizedReaderId)
            reader?.let { quranReader ->
                Constants.getSoraLink(quranReader.server, surahId)
            } ?: throw Exception("Reader not found with ID: $normalizedReaderId")
        } catch (e: Exception) {
            val normalizedReaderId = normalizeToAsciiDigits(readerId)
            "https://www.mp3quran.net/api/reader/$normalizedReaderId/${
                String.format(
                    Locale.US,
                    "%03d",
                    surahId
                )
            }.mp3"
        }
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

    suspend fun downloadAudio(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String,
        verseId: Int? = null,
    ): Boolean {
        return try {
            val audioId = if (verseId != null) {
                "${readerId}_${surahId}_${verseId}"
            } else {
                "${readerId}_${surahId}"
            }

            getDownloadedAudio(readerId, surahId, verseId)?.let {
                if (it.isComplete && File(it.localFilePath).exists()) {
                    return true
                }
            }

            val fileName = if (verseId != null) {
                "${surahName}_آية_${verseId}_${readerName}.mp3"
            } else {
                "${surahName}_${readerName}.mp3"
            }

            val folder = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "offline_audio/$readerName"
            )
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val localFile = File(folder, fileName)

            val downloadManager =
                context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(audioUrl))
                .setDestinationUri(Uri.fromFile(localFile))
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setTitle("Downloading $surahName")
                .setDescription("$readerName - $surahName")
                .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI or DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            val downloadId = downloadManager.enqueue(request)

            val downloadedAudio = DownloadedAudio(
                id = audioId,
                readerId = readerId,
                surahId = surahId,
                verseId = verseId,
                surahName = surahName,
                readerName = readerName,
                localFilePath = localFile.absolutePath,
                originalUrl = audioUrl,
                downloadDate = Date(),
                fileSize = 0L,
                isComplete = false,
                downloadType = if (verseId != null) DownloadType.INDIVIDUAL_VERSE else DownloadType.FULL_SURAH
            )

            downloadedAudioDao.insertDownloadedAudio(downloadedAudio)
            monitorDownload(downloadId, audioId, localFile)

            true
        } catch (e: Exception) {
            false
        }
    }


    suspend fun getDownloadedAudio(
        readerId: String,
        surahId: Int,
        verseId: Int? = null,
    ): DownloadedAudio? {
        return downloadedAudioDao.getDownloadedAudio(readerId, surahId, verseId)
    }

    suspend fun getDownloadedAudioByReader(readerId: String): List<DownloadedAudio> {
        return downloadedAudioDao.getDownloadedAudioByReader(readerId)
    }

    suspend fun getAllDownloadedAudio(): List<DownloadedAudio> {
        return downloadedAudioDao.getAllDownloadedAudio()
    }

    suspend fun isAudioDownloaded(readerId: String, surahId: Int): Boolean {
        val downloaded = getDownloadedAudio(readerId, surahId)
        return downloaded?.isComplete == true && File(downloaded.localFilePath).exists()
    }

    suspend fun getLocalAudioPath(readerId: String, surahId: Int): String? {
        val downloaded = getDownloadedAudio(readerId, surahId)
        return if (downloaded?.isComplete == true && File(downloaded.localFilePath).exists()) {
            downloaded.localFilePath
        } else null
    }

    suspend fun deleteDownloadedAudio(readerId: String, surahId: Int) {
        getDownloadedAudio(readerId, surahId)?.let { audio ->
            File(audio.localFilePath).delete()
            downloadedAudioDao.deleteDownloadedAudio(audio)
        }
    }

    suspend fun deleteAllDownloadedAudio(readerId: String) {
        val audioList = getDownloadedAudioByReader(readerId)
        audioList.forEach { audio ->
            File(audio.localFilePath).delete()
        }
        downloadedAudioDao.deleteAllByReader(readerId)
    }
}