package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.repository.QuranListenerReaderRepository
import com.megahed.eqtarebmenalla.db.repository.SoraSongRepository
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import com.megahed.eqtarebmenalla.offline.SmartAudioUrlHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@HiltViewModel
class QuranListenerReaderViewModel @Inject constructor(
    private val quranListenerReaderRepository: QuranListenerReaderRepository,
    private val songRepository: SoraSongRepository,
    private val offlineAudioManager: OfflineAudioManager
) : ViewModel() {

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    // Cache for frequently accessed data
    private val downloadStatusCache = ConcurrentHashMap<String, Boolean>()
    private val readerCache = ConcurrentHashMap<String, QuranListenerReader>()
    private val soraCache = ConcurrentHashMap<String, List<SoraSong>>()

    // Coroutine scope for background operations
    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        // Initialize download progress monitoring
        initializeDownloadProgressMonitoring()
    }

    private fun initializeDownloadProgressMonitoring() {
        viewModelScope.launch {
            offlineAudioManager.downloadProgress
                .collect { progressMap ->
                    _downloadProgress.value = progressMap
                }
        }
    }

    override fun onCleared() {
        super.onCleared()
        backgroundScope.cancel()
    }

    fun getOfflineAudioManager(): OfflineAudioManager = offlineAudioManager

    // Optimized reader data loading with caching
    suspend fun getQuranListenerReaderById(id: String): QuranListenerReader? {
        return try {
            // Check cache first
            readerCache[id]?.let { return it }

            // Load from repository
            val reader = quranListenerReaderRepository.getQuranListenerReaderById(id)
            reader?.let {
                // Cache the result
                readerCache[id] = it

                // Asynchronously prepare sora data
                backgroundScope.launch {
                    prepareReaderData(it)
                }
            }
            reader
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun prepareReaderData(quranListenerReader: QuranListenerReader) {
        try {
            val surahIds = quranListenerReader.suras.split(",").mapNotNull {
                it.trim().toIntOrNull()
            }

            // Process in batches to avoid overwhelming the system
            surahIds.chunked(10).forEach { batch ->
                batch.forEach { surahId ->
                    processOrCreateSoraSong(surahId, quranListenerReader)
                }
                delay(50) // Small delay between batches
            }
        } catch (e: Exception) {
            // Log error but don't crash
        }
    }

    private suspend fun processOrCreateSoraSong(surahId: Int, reader: QuranListenerReader) {
        try {
            val existingSong = songRepository.getSoraSongById(surahId, reader.id)

            if (existingSong != null) {
                // Update existing song with optimized URL
                val audioUrl = SmartAudioUrlHelper.getAudioUrl(
                    offlineAudioManager = offlineAudioManager,
                    readerId = reader.id,
                    surahId = surahId,
                    verseId = null,
                    onlineBaseUrl = reader.server
                )

                if (existingSong.url != audioUrl) {
                    existingSong.url = audioUrl
                    songRepository.updateSoraSong(existingSong)
                }
            } else {
                // Create new song
                val audioUrl = SmartAudioUrlHelper.getAudioUrl(
                    offlineAudioManager = offlineAudioManager,
                    readerId = reader.id,
                    surahId = surahId,
                    verseId = null,
                    onlineBaseUrl = reader.server
                )

                val soraSong = SoraSong(surahId, reader.id, audioUrl, false)
                songRepository.insertSoraSong(soraSong)
            }
        } catch (e: Exception) {
            // Handle individual failures without stopping the batch
        }
    }

    // Optimized favorite update without full object update
    suspend fun updateQuranListenerReaderFavorite(reader: QuranListenerReader) {
        try {
            quranListenerReaderRepository.updateQuranListenerReader(reader)
            readerCache[reader.id] = reader // Update cache
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun updateSoraSongFavorite(soraSong: SoraSong) {
        try {
            songRepository.updateSoraSong(soraSong)
            // Invalidate cache for this reader
            soraCache.remove(soraSong.readerId)
        } catch (e: Exception) {
            throw e
        }
    }

    // Cached sora songs with flow optimization
    fun getSongsOfSoraCached(readerId: String): Flow<List<SoraSong>> {
        return songRepository.getSongsOfSora(readerId)
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .onEach { songs ->
                // Update cache
                soraCache[readerId] = songs
            }
    }

    // Fast download status check with caching
    suspend fun isSurahDownloadedCached(readerId: String, surahId: Int): Boolean {
        val cacheKey = "${readerId}_$surahId"

        // Check cache first
        downloadStatusCache[cacheKey]?.let { return it }

        // Check actual status and cache result
        val isDownloaded = try {
            offlineAudioManager.isSurahDownloaded(readerId, surahId)
        } catch (e: Exception) {
            false
        }

        downloadStatusCache[cacheKey] = isDownloaded
        return isDownloaded
    }

    // Batch download status check for better performance
    suspend fun getBulkDownloadStatuses(readerId: String, surahIds: List<Int>): List<Boolean> {
        return withContext(Dispatchers.IO) {
            surahIds.map { surahId ->
                isSurahDownloadedCached(readerId, surahId)
            }
        }
    }

    // Optimized single audio download for Android 15
    suspend fun downloadAudioOptimized(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String
    ): Boolean {
        return try {
            // Clear download status cache for this item
            val cacheKey = "${readerId}_$surahId"
            downloadStatusCache.remove(cacheKey)

            // Start download with enhanced error handling
            val success = offlineAudioManager.downloadSurahAudioOptimized(
                readerId = readerId,
                surahId = surahId,
                surahName = surahName,
                readerName = readerName,
                audioUrl = audioUrl
            )

            if (success) {
                // Start monitoring download completion
                backgroundScope.launch {
                    monitorDownloadCompletion(readerId, surahId)
                }
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun monitorDownloadCompletion(readerId: String, surahId: Int) {
        try {
            var attempts = 0
            val maxAttempts = 60 // 2 minutes

            while (attempts < maxAttempts) {
                delay(2000) // Check every 2 seconds
                attempts++

                if (offlineAudioManager.isSurahDownloaded(readerId, surahId)) {
                    // Update cache
                    downloadStatusCache["${readerId}_$surahId"] = true

                    // Update song URL to local path if needed
                    updateSongToLocalPath(readerId, surahId)
                    break
                }
            }
        } catch (e: Exception) {
            // Silent failure
        }
    }

    private suspend fun updateSongToLocalPath(readerId: String, surahId: Int) {
        try {
            val localPath = offlineAudioManager.getOfflineAudioUrl(readerId, surahId)
            if (localPath != null) {
                val soraSong = songRepository.getSoraSongById(surahId, readerId)
                soraSong?.let {
                    it.url = localPath
                    songRepository.updateSoraSong(it)
                }
            }
        } catch (e: Exception) {
            // Silent failure
        }
    }

    // Optimized bulk download with progress tracking
    suspend fun downloadAllSoraSongsOptimized(
        readerId: String,
        readerName: String
    ): Boolean {
        return try {
            val reader = getQuranListenerReaderById(readerId) ?: return false
            val surahIds = reader.suras.split(",").mapNotNull { it.trim().toIntOrNull() }

            // Clear cache for all items
            surahIds.forEach { surahId ->
                downloadStatusCache.remove("${readerId}_$surahId")
            }

            // Filter out already downloaded surahs
            val undownloadedSurahs = surahIds.filter { surahId ->
                !offlineAudioManager.isSurahDownloaded(readerId, surahId)
            }

            if (undownloadedSurahs.isEmpty()) return true

            // Start bulk download
            val success = offlineAudioManager.downloadBulkSurahsOptimized(
                readerId = readerId,
                surahIds = undownloadedSurahs,
                readerName = readerName,
                serverUrl = reader.server
            )

            if (success) {
                // Monitor bulk download progress
                backgroundScope.launch {
                    monitorBulkDownload(readerId, undownloadedSurahs)
                }
            }

            success
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun monitorBulkDownload(readerId: String, surahIds: List<Int>) {
        try {
            var attempts = 0
            val maxAttempts = 120 // 4 minutes for bulk

            while (attempts < maxAttempts) {
                delay(2000)
                attempts++

                // Check completion status
                val completedCount = surahIds.count { surahId ->
                    offlineAudioManager.isSurahDownloaded(readerId, surahId)
                }

                // Update cache for completed downloads
                surahIds.forEach { surahId ->
                    if (offlineAudioManager.isSurahDownloaded(readerId, surahId)) {
                        downloadStatusCache["${readerId}_$surahId"] = true
                    }
                }

                if (completedCount == surahIds.size) break
            }
        } catch (e: Exception) {
            // Silent failure
        }
    }

    // Optimized deletion methods
    suspend fun deleteDownloadedAudioOptimized(readerId: String, surahId: Int) {
        try {
            offlineAudioManager.deleteDownloadedAudioOptimized(readerId, surahId)

            // Update cache
            downloadStatusCache["${readerId}_$surahId"] = false

            // Update song URL back to online URL
            backgroundScope.launch {
                updateSongToOnlineUrl(readerId, surahId)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun deleteAllDownloadedAudioOptimized(readerId: String) {
        try {
            offlineAudioManager.deleteAllDownloadedAudioOptimized(readerId)

            // Clear cache for this reader
            downloadStatusCache.keys.removeAll { key ->
                key.startsWith("${readerId}_")
            }

            // Update all songs back to online URLs
            backgroundScope.launch {
                updateAllSongsToOnlineUrls(readerId)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    private suspend fun updateSongToOnlineUrl(readerId: String, surahId: Int) {
        try {
            val reader = getQuranListenerReaderById(readerId) ?: return
            val soraSong = songRepository.getSoraSongById(surahId, readerId) ?: return

            val onlineUrl = Constants.getSoraLink(reader.server, surahId)
            if (soraSong.url != onlineUrl) {
                soraSong.url = onlineUrl
                songRepository.updateSoraSong(soraSong)
            }
        } catch (e: Exception) {
            // Silent failure
        }
    }

    private suspend fun updateAllSongsToOnlineUrls(readerId: String) {
        try {
            val reader = getQuranListenerReaderById(readerId) ?: return
            prepareReaderData(reader) // This will reset URLs to online versions
        } catch (e: Exception) {
            // Silent failure
        }
    }

    // Legacy methods maintained for compatibility
    fun updateQuranListenerReader(quranListenerReader: QuranListenerReader) {
        viewModelScope.launch {
            updateQuranListenerReaderFavorite(quranListenerReader)
        }
    }

    fun insertSoraSong(soraSong: SoraSong) {
        viewModelScope.launch {
            songRepository.insertSoraSong(soraSong)
            soraCache.remove(soraSong.readerId) // Invalidate cache
        }
    }

    fun updateSoraSong(soraSong: SoraSong) {
        viewModelScope.launch {
            updateSoraSongFavorite(soraSong)
        }
    }

    suspend fun getSoraSongById(id: Int, readerId: String): SoraSong? {
        return songRepository.getSoraSongById(id, readerId)
    }

    fun getFavoriteSoraSong(): Flow<List<SoraSong>> {
        return songRepository.getFavoriteSoraSong()
    }

    fun getSongsOfSora(readerId: String): Flow<List<SoraSong>> {
        return getSongsOfSoraCached(readerId)
    }

    fun getAllFavSorasOfReader(): Flow<List<ReaderWithSora>> {
        return quranListenerReaderRepository.getAllFavSorasOfReader()
    }

    @Deprecated("Use downloadAudioOptimized instead")
    fun downloadAudio(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String
    ): Boolean {
        viewModelScope.launch {
            downloadAudioOptimized(readerId, surahId, surahName, readerName, audioUrl)
        }
        return true
    }

    suspend fun isAudioDownloaded(readerId: String, surahId: Int): Boolean {
        return isSurahDownloadedCached(readerId, surahId)
    }

    @Deprecated("Use deleteDownloadedAudioOptimized instead")
    suspend fun deleteDownloadedAudio(readerId: String, surahId: Int) {
        deleteDownloadedAudioOptimized(readerId, surahId)
    }

    @Deprecated("Use deleteAllDownloadedAudioOptimized instead")
    suspend fun deleteAllDownloadedAudio(readerId: String) {
        deleteAllDownloadedAudioOptimized(readerId)
    }

    @Deprecated("Use downloadAllSoraSongsOptimized instead")
    suspend fun downloadAllSoraSongs(
        readerId: String,
        readerName: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean {
        return downloadAllSoraSongsOptimized(readerId, readerName)
    }
}