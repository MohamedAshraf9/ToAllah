package com.megahed.eqtarebmenalla.offline

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.dao.DownloadedAudioDao
import com.megahed.eqtarebmenalla.db.dao.OfflineSettingsDao
import com.megahed.eqtarebmenalla.db.model.DownloadType
import com.megahed.eqtarebmenalla.db.model.DownloadedAudio
import com.megahed.eqtarebmenalla.db.model.OfflineSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

@Singleton
class OfflineAudioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val downloadedAudioDao: DownloadedAudioDao,
    private val offlineSettingsDao: OfflineSettingsDao,
) {
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    private val downloadStatusCache = ConcurrentHashMap<String, Boolean>()
    private val activeDownloads = ConcurrentHashMap<String, Long>()
    private val downloadRetryCount = ConcurrentHashMap<String, Int>()

    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val networkCallback = NetworkCallback()

    private val downloadMonitorScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val downloadBroadcastReceiver = DownloadBroadcastReceiver()

    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 5000L
        private const val DOWNLOAD_TIMEOUT_MS = 300000L
        private const val BATCH_SIZE = 5
        private const val CONCURRENT_DOWNLOADS = 3
    }

    init {
        initializeNetworkMonitoring()
        initializeDownloadReceiver()
    }

    private fun initializeNetworkMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }

    private fun initializeDownloadReceiver() {
        val filter = IntentFilter().apply {
            addAction(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            addAction(DownloadManager.ACTION_NOTIFICATION_CLICKED)
        }
        ContextCompat.registerReceiver(
            context,
            downloadBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    fun isNetworkAvailable(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) &&
                        (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
            } else {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnectedOrConnecting == true
            }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isSurahDownloaded(readerId: String, surahId: Int): Boolean {
        val cacheKey = "${readerId}_$surahId"

        downloadStatusCache[cacheKey]?.let { return it }

        val isDownloaded = try {
            val audio = downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
            val result = audio?.isComplete == true &&
                    File(audio.localFilePath).exists() &&
                    File(audio.localFilePath).length() > 0

            downloadStatusCache[cacheKey] = result
            result
        } catch (e: Exception) {
            downloadStatusCache[cacheKey] = false
            false
        }

        return isDownloaded
    }

    suspend fun downloadSurahAudioOptimized(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String,
    ): Boolean {
        return downloadAudioWithRetry(
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

    suspend fun downloadBulkSurahsOptimized(
        readerId: String,
        surahIds: List<Int>,
        readerName: String,
        serverUrl: String,
    ): Boolean {
        return try {
            val semaphore = Semaphore(CONCURRENT_DOWNLOADS)
            val jobs = mutableListOf<Deferred<Boolean>>()

            surahIds.chunked(BATCH_SIZE).forEach { batch ->
                batch.forEach { surahId ->
                    val job = downloadMonitorScope.async {
                        semaphore.withPermit {
                            val audioUrl = Constants.getSoraLink(serverUrl, surahId)
                            val surahName = Constants.SORA_OF_QURAN[surahId]

                            downloadSurahAudioOptimized(
                                readerId = readerId,
                                surahId = surahId,
                                surahName = surahName,
                                readerName = readerName,
                                audioUrl = audioUrl
                            )
                        }
                    }
                    jobs.add(job)
                }

                delay(1000)
            }

            val results = withTimeoutOrNull(DOWNLOAD_TIMEOUT_MS) {
                jobs.awaitAll()
            }

            results?.all { it } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun downloadAudioWithRetry(
        audioId: String,
        readerId: String,
        surahId: Int,
        verseId: Int?,
        audioName: String,
        readerName: String,
        audioUrl: String,
        downloadType: DownloadType,
        retryCount: Int = 0,
    ): Boolean {
        try {
            val existing = if (verseId != null) {
                downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
            } else {
                downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
            }

            if (existing?.isComplete == true && File(existing.localFilePath).exists()) {
                return true
            }

            if (!isNetworkAvailable()) {
                scheduleDownloadForLater(audioId, readerId, surahId, verseId, audioName, readerName, audioUrl, downloadType)
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

            val request = createEnhancedDownloadRequest(audioUrl, localFile, audioName, readerName)

            val downloadId = downloadManager.enqueue(request)

            activeDownloads[audioId] = downloadId

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

            monitorDownloadEnhanced(downloadId, audioId, localFile, retryCount)

            return true
        } catch (e: Exception) {
            if (retryCount < MAX_RETRY_ATTEMPTS) {
                delay(RETRY_DELAY_MS)
                return downloadAudioWithRetry(
                    audioId, readerId, surahId, verseId, audioName,
                    readerName, audioUrl, downloadType, retryCount + 1
                )
            }
            return false
        }
    }

    private fun createEnhancedDownloadRequest(
        audioUrl: String,
        localFile: File,
        audioName: String,
        readerName: String,
    ): DownloadManager.Request {
        return DownloadManager.Request(Uri.parse(audioUrl)).apply {
            setDestinationUri(Uri.fromFile(localFile))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setTitle("تحميل $audioName")
            setDescription("$readerName - $audioName")

            setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI or
                        DownloadManager.Request.NETWORK_MOBILE
            )
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)

            addRequestHeader("User-Agent", "QuranApp/1.0")
            addRequestHeader("Accept", "audio/*")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                setRequiresCharging(false)
                setRequiresDeviceIdle(false)
            }
        }
    }

    private fun scheduleDownloadForLater(
        audioId: String,
        readerId: String,
        surahId: Int,
        verseId: Int?,
        audioName: String,
        readerName: String,
        audioUrl: String,
        downloadType: DownloadType,
    ) {
        downloadMonitorScope.launch {
            while (!isNetworkAvailable()) {
                delay(10000)
            }

            downloadAudioWithRetry(
                audioId, readerId, surahId, verseId,
                audioName, readerName, audioUrl, downloadType
            )
        }
    }

    private fun monitorDownloadEnhanced(downloadId: Long, audioId: String, localFile: File, retryCount: Int) {
        downloadMonitorScope.launch {
            try {
                var lastProgress = 0
                var stagnantCount = 0
                val maxStagnantChecks = 10

                while (true) {
                    val cursor = downloadManager.query(DownloadManager.Query().setFilterById(downloadId))

                    if (cursor != null && cursor.moveToFirst()) {
                        val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                        val downloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                        val total = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))

                        if (total > 0L) {
                            val progress = ((downloaded * 100) / total).toInt().coerceIn(0, 100)

                            if (progress == lastProgress) {
                                stagnantCount++
                            } else {
                                stagnantCount = 0
                                lastProgress = progress
                            }

                            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                                put(audioId, progress)
                            }
                        }

                        when (status) {
                            DownloadManager.STATUS_SUCCESSFUL -> {
                                handleDownloadSuccess(audioId, localFile)
                                cursor.close()
                                return@launch
                            }

                            DownloadManager.STATUS_FAILED -> {
                                handleDownloadFailure(audioId, downloadId, reason, retryCount)
                                cursor.close()
                                return@launch
                            }

                            DownloadManager.STATUS_PAUSED -> {
                                if (reason == DownloadManager.PAUSED_WAITING_FOR_NETWORK) {
                                    // Wait for network
                                    delay(5000)
                                }
                            }
                        }

                        if (stagnantCount >= maxStagnantChecks) {
                            handleStagnantDownload(downloadId, audioId, retryCount)
                            cursor.close()
                            return@launch
                        }
                    } else {

                        cleanupFailedDownload(audioId)
                        return@launch
                    }

                    cursor?.close()
                    delay(2000)
                }
            } catch (e: Exception) {
                cleanupFailedDownload(audioId)
            }
        }
    }

    private suspend fun handleDownloadSuccess(audioId: String, localFile: File) {
        try {
            val parts = audioId.split("_")
            if (parts.size >= 2) {
                val readerId = parts[0]
                val surahId = parts[1].toIntOrNull()
                val verseId = if (parts.size >= 3) parts[2].toIntOrNull() else null

                if (surahId != null) {
                    val existingAudio = if (verseId != null) {
                        downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
                    } else {
                        downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
                    }

                    existingAudio?.let {
                        downloadedAudioDao.insertDownloadedAudio(
                            it.copy(
                                isComplete = true,
                                fileSize = localFile.length()
                            )
                        )
                    }

                    downloadStatusCache["${readerId}_$surahId"] = true
                }
            }
        } finally {
            activeDownloads.remove(audioId)
            downloadRetryCount.remove(audioId)
            _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
                remove(audioId)
            }
        }
    }

    private suspend fun handleDownloadFailure(audioId: String, downloadId: Long, reason: Int, retryCount: Int) {
        activeDownloads.remove(audioId)

        val shouldRetry = when (reason) {
            DownloadManager.ERROR_HTTP_DATA_ERROR,
            DownloadManager.ERROR_TOO_MANY_REDIRECTS,
            DownloadManager.ERROR_UNHANDLED_HTTP_CODE,
                -> retryCount < MAX_RETRY_ATTEMPTS
            DownloadManager.ERROR_CANNOT_RESUME -> false
            DownloadManager.ERROR_DEVICE_NOT_FOUND -> false
            DownloadManager.ERROR_FILE_ALREADY_EXISTS -> false
            else -> retryCount < MAX_RETRY_ATTEMPTS
        }

        if (shouldRetry) {
            downloadRetryCount[audioId] = retryCount + 1
            delay(RETRY_DELAY_MS)
        } else {
            cleanupFailedDownload(audioId)
        }
    }

    private suspend fun handleStagnantDownload(downloadId: Long, audioId: String, retryCount: Int) {

        downloadManager.remove(downloadId)

        if (retryCount < MAX_RETRY_ATTEMPTS) {
            downloadRetryCount[audioId] = retryCount + 1
            delay(RETRY_DELAY_MS)

        } else {
            cleanupFailedDownload(audioId)
        }
    }

    private suspend fun cleanupFailedDownload(audioId: String) {
        activeDownloads.remove(audioId)
        downloadRetryCount.remove(audioId)
        _downloadProgress.value = _downloadProgress.value.toMutableMap().apply {
            remove(audioId)
        }

        val parts = audioId.split("_")
        if (parts.size >= 2) {
            val readerId = parts[0]
            val surahId = parts[1].toIntOrNull()
            if (surahId != null) {
                downloadStatusCache["${readerId}_$surahId"] = false
            }
        }
    }

    suspend fun deleteDownloadedAudioOptimized(readerId: String, surahId: Int) {
        try {
            val audio = downloadedAudioDao.getDownloadedSurahAudio(readerId, surahId)
            audio?.let {

                val file = File(it.localFilePath)
                if (file.exists()) {
                    file.delete()
                }

                downloadedAudioDao.deleteDownloadedAudio(it)

                downloadStatusCache["${readerId}_$surahId"] = false
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteAllDownloadedAudioOptimized(readerId: String) {
        try {
            val audioList = downloadedAudioDao.getDownloadedAudioByReader(readerId)

            audioList.map { audio ->
                downloadMonitorScope.async {
                    val file = File(audio.localFilePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }.awaitAll()

            downloadedAudioDao.deleteAllByReader(readerId)

            downloadStatusCache.keys.removeAll { key ->
                key.startsWith("${readerId}_")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private inner class NetworkCallback : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            downloadMonitorScope.launch {
                resumePendingDownloads()
            }
        }

        override fun onLost(network: Network) {
            super.onLost(network)
        }
    }

    private suspend fun resumePendingDownloads() {
        try {
            val incompleteDownloads = downloadedAudioDao.getAllDownloadedAudio()
                .filter { !it.isComplete }

            incompleteDownloads.forEach { audio ->
                if (!activeDownloads.containsKey(audio.id)) {
                    downloadAudioWithRetry(
                        audioId = audio.id,
                        readerId = audio.readerId,
                        surahId = audio.surahId,
                        verseId = audio.verseId,
                        audioName = audio.surahName,
                        readerName = audio.readerName,
                        audioUrl = audio.originalUrl,
                        downloadType = audio.downloadType
                    )
                }
            }
        } catch (e: Exception) {

        }
    }

    private inner class DownloadBroadcastReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                DownloadManager.ACTION_DOWNLOAD_COMPLETE -> {
                    val downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                    if (downloadId != -1L) {
                        handleDownloadCompleteEvent(downloadId)
                    }
                }
            }
        }
    }

    private fun handleDownloadCompleteEvent(downloadId: Long) {
        downloadMonitorScope.launch {
            try {
                val audioId = activeDownloads.entries.find { it.value == downloadId }?.key
                audioId?.let {
                }
            } catch (e: Exception) {

            }
        }
    }

    suspend fun getOfflineAudioUrl(readerId: String, surahId: Int, verseId: Int? = null): String? {
        return try {
            if (verseId != null) {
                val verseAudio = downloadedAudioDao.getDownloadedVerseAudio(readerId, surahId, verseId)
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

    @Deprecated("Use downloadSurahAudioOptimized instead")
    suspend fun downloadSurahAudio(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String,
    ): Boolean {
        return downloadSurahAudioOptimized(readerId, surahId, surahName, readerName, audioUrl)
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

    fun cleanup() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.unregisterNetworkCallback(networkCallback)
            }
            context.unregisterReceiver(downloadBroadcastReceiver)
            downloadMonitorScope.cancel()
        } catch (_: Exception) {
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

    suspend fun getAllDownloadedAudio(): List<DownloadedAudio> {
        return downloadedAudioDao.getAllDownloadedAudio()
    }
    suspend fun getDownloadedAudio(
        readerId: String,
        surahId: Int,
        verseId: Int? = null,
    ): DownloadedAudio? {
        return downloadedAudioDao.getDownloadedAudio(readerId, surahId, verseId)
    }

    suspend fun isAudioDownloaded(readerId: String, surahId: Int): Boolean {
        val downloaded = getDownloadedAudio(readerId, surahId)
        return downloaded?.isComplete == true && File(downloaded.localFilePath).exists()
    }
    suspend fun getDownloadedAudioByReader(readerId: String): List<DownloadedAudio> {
        return downloadedAudioDao.getDownloadedAudioByReader(readerId)
    }
}