package com.megahed.eqtarebmenalla.feature_data.presentation.ui.quranListenerReader

import android.annotation.SuppressLint
import android.util.Log
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranListenerReaderViewModel @Inject constructor(
    private val quranListenerReaderRepository: QuranListenerReaderRepository,
    private val songRepository: SoraSongRepository,
    private val offlineAudioManager: OfflineAudioManager
) : ViewModel() {

    private val _downloadProgress = MutableStateFlow<Map<String, Int>>(emptyMap())
    val downloadProgress: StateFlow<Map<String, Int>> = _downloadProgress.asStateFlow()

    init {
        viewModelScope.launch {
            offlineAudioManager.downloadProgress.collect { progressMap ->
                _downloadProgress.value = progressMap
            }
        }
    }

    private suspend fun getData(quranListenerReader: QuranListenerReader) {
        val arr = quranListenerReader.suras.split(",")
        val ints = arr.map { it.toInt() }

        ints.forEach { surahId ->
            viewModelScope.launch {
                songRepository.getSoraSongById(surahId, quranListenerReader.id)?.let { soraSong ->
                    val audioUrl = SmartAudioUrlHelper.getAudioUrl(
                        offlineAudioManager = offlineAudioManager,
                        readerId = quranListenerReader.id,
                        surahId = surahId,
                        verseId = null,
                        onlineBaseUrl = quranListenerReader.server
                    )

                    soraSong.readerId = quranListenerReader.id
                    soraSong.url = audioUrl
                    songRepository.updateSoraSong(soraSong)

                } ?: run {

                    viewModelScope.launch {
                        val audioUrl = SmartAudioUrlHelper.getAudioUrl(
                            offlineAudioManager = offlineAudioManager,
                            readerId = quranListenerReader.id,
                            surahId = surahId,
                            verseId = null,
                            onlineBaseUrl = quranListenerReader.server
                        )

                        val soraSong = SoraSong(surahId, quranListenerReader.id, audioUrl, false)
                        songRepository.insertSoraSong(soraSong)
                    }
                }
            }
        }
    }

    fun getOfflineAudioManager(): OfflineAudioManager {
        return offlineAudioManager
    }

    suspend fun getQuranListenerReaderById(id: String): QuranListenerReader? {
        return quranListenerReaderRepository.getQuranListenerReaderById(id)?.let {
            getData(it)
            it
        }
    }

    fun updateQuranListenerReader(quranListenerReader: QuranListenerReader) {
        viewModelScope.launch {
            quranListenerReaderRepository.updateQuranListenerReader(quranListenerReader)
        }
    }

    fun insertSoraSong(soraSong: SoraSong) {
        viewModelScope.launch {
            songRepository.insertSoraSong(soraSong)
        }
    }

    fun updateSoraSong(soraSong: SoraSong) {
        viewModelScope.launch {
            songRepository.updateSoraSong(soraSong)
        }
    }

    suspend fun getSoraSongById(id: Int, readerId: String): SoraSong? {
        return songRepository.getSoraSongById(id, readerId)
    }

    fun getFavoriteSoraSong(): Flow<List<SoraSong>> {
        return songRepository.getFavoriteSoraSong()
    }

    fun getSongsOfSora(readerId: String): Flow<List<SoraSong>> {
        return songRepository.getSongsOfSora(readerId)
    }

    fun getAllFavSorasOfReader(): Flow<List<ReaderWithSora>> {
        return quranListenerReaderRepository.getAllFavSorasOfReader()
    }

    @SuppressLint("LongLogTag")
    fun downloadAudio(
        readerId: String,
        surahId: Int,
        surahName: String,
        readerName: String,
        audioUrl: String
    ): Boolean {
        viewModelScope.launch {
            try {
                val success = offlineAudioManager.downloadSurahAudio(
                    readerId = readerId,
                    surahId = surahId,
                    surahName = surahName,
                    readerName = readerName,
                    audioUrl = audioUrl
                )

                if (success) {
                    monitorDownloadCompletion(readerId, surahId)
                }
            } catch (e: Exception) {
                Log.e("QuranListenerReaderViewModel", "Error downloading audio", e)
            }
        }
        return true
    }

    @SuppressLint("LongLogTag")
    private fun monitorDownloadCompletion(readerId: String, surahId: Int) {
        viewModelScope.launch {
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
            }
        }
    }

    suspend fun isAudioDownloaded(readerId: String, surahId: Int): Boolean {
        return offlineAudioManager.isSurahDownloaded(readerId, surahId)
    }

    @SuppressLint("LongLogTag")
    suspend fun deleteDownloadedAudio(readerId: String, surahId: Int) {
        try {
            offlineAudioManager.deleteDownloadedAudio(readerId, surahId)

            val soraSong = songRepository.getSoraSongById(surahId, readerId)
            soraSong?.let {
                val quranListenerReader =
                    quranListenerReaderRepository.getQuranListenerReaderById(readerId)
                quranListenerReader?.let { reader ->
                    it.url = Constants.getSoraLink(reader.server, surahId)
                    viewModelScope.launch {
                        songRepository.updateSoraSong(it)
                    }
                }
            }
        } catch (e: Exception) {
        }
    }

    @SuppressLint("LongLogTag")
    suspend fun deleteAllDownloadedAudio(readerId: String) {
        try {
            offlineAudioManager.deleteAllDownloadedAudio(readerId)

            viewModelScope.launch {
                val quranListenerReader =
                    quranListenerReaderRepository.getQuranListenerReaderById(readerId)
                quranListenerReader?.let { reader ->
                    getData(reader)
                }
            }
        } catch (e: Exception) {
        }
    }

    @SuppressLint("LongLogTag")
    suspend fun downloadAllSoraSongs(
        readerId: String,
        readerName: String,
        onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }
    ): Boolean {
        return try {
            val quranListenerReader =
                quranListenerReaderRepository.getQuranListenerReaderById(readerId)
            quranListenerReader?.let { reader ->
                val surahIds = reader.suras.split(",").map { it.toInt() }
                var completed = 0

                surahIds.forEach { surahId ->
                    if (!offlineAudioManager.isSurahDownloaded(readerId, surahId)) {
                        val audioUrl = Constants.getSoraLink(reader.server, surahId)
                        val surahName = Constants.SORA_OF_QURAN[surahId] ?: "Surah $surahId"

                        val success = offlineAudioManager.downloadSurahAudio(
                            readerId = readerId,
                            surahId = surahId,
                            surahName = surahName,
                            readerName = readerName,
                            audioUrl = audioUrl
                        )

                        if (success) {
                            completed++
                            onProgress(completed, surahIds.size)
                        }
                    }
                }

                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
}