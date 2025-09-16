package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class QuranListenerReaderAdapter(
    private val context: Context,
    private val onItemClickListener: OnItemWithFavClickListener<SoraSong>,
    private val offlineAudioManager: OfflineAudioManager,
    private val readerId: String,
    private val lifecycleScope: LifecycleCoroutineScope
) : ListAdapter<SoraSong, QuranListenerReaderAdapter.MyHolder>(SurahDiffCallback()), Filterable {

    private val downloadStatusCache = ConcurrentHashMap<Int, Boolean>()
    private val downloadProgressCache = ConcurrentHashMap<String, Int>()
    private val favoriteStatusCache = ConcurrentHashMap<Int, Boolean>()

    private var originalData = listOf<SoraSong>()
    private var currentFilter = ""

    private val uiHandler = Handler(Looper.getMainLooper())
    private val pendingUpdates = ConcurrentHashMap<String, Runnable>()

    companion object {
        private const val DEBOUNCE_DELAY = 100L
        private const val BATCH_UPDATE_SIZE = 10
    }

    fun submitSoraList(data: List<SoraSong>) {
        lifecycleScope.launch {

            val statusMap = withContext(Dispatchers.IO) {
                loadDownloadStatusesBatch(data.map { it.SoraId })
            }

            withContext(Dispatchers.Main) {
                downloadStatusCache.putAll(statusMap)
                favoriteStatusCache.clear()
                data.forEach { song ->
                    favoriteStatusCache[song.SoraId] = song.isVaForte
                }

                originalData = data
                applyCurrentFilter()
            }
        }
    }

    private suspend fun loadDownloadStatusesBatch(surahIds: List<Int>): Map<Int, Boolean> {
        return withContext(Dispatchers.IO) {
            val statusMap = mutableMapOf<Int, Boolean>()

            surahIds.chunked(BATCH_UPDATE_SIZE).forEach { batch ->
                batch.forEach { surahId ->
                    try {
                        val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, surahId)
                        statusMap[surahId] = isDownloaded
                    } catch (e: Exception) {
                        statusMap[surahId] = false
                    }
                }
                delay(10)
            }

            statusMap
        }
    }

    private fun applyCurrentFilter() {
        val filteredData = if (currentFilter.isEmpty()) {
            originalData
        } else {
            originalData.filter { soraSong ->
                Constants.SORA_OF_QURAN[soraSong.SoraId]
                    .lowercase(Locale.getDefault())
                    .contains(currentFilter.lowercase(Locale.getDefault()).trim())
            }
        }

        super.submitList(filteredData) {
        }
    }

    fun updateDownloadProgress(progressMap: Map<String, Int>) {
        downloadProgressCache.putAll(progressMap)

        val updateKey = "progress_update"
        pendingUpdates[updateKey]?.let { uiHandler.removeCallbacks(it) }

        val updateRunnable = Runnable {
            val currentList = currentList
            val updatedPositions = mutableListOf<Int>()

            currentList.forEachIndexed { index, soraSong ->
                val downloadId = "${readerId}_${soraSong.SoraId}"
                if (progressMap.containsKey(downloadId)) {
                    updatedPositions.add(index)
                }
            }

            updatedPositions.forEach { position ->
                notifyItemChanged(position, "progress_update")
            }

            pendingUpdates.remove(updateKey)
        }

        pendingUpdates[updateKey] = updateRunnable
        uiHandler.postDelayed(updateRunnable, DEBOUNCE_DELAY)
    }

    fun updateFavoriteStatus(surahId: Int, isFavorite: Boolean) {
        favoriteStatusCache[surahId] = isFavorite

        val position = currentList.indexOfFirst { it.SoraId == surahId }
        if (position != -1) {
            notifyItemChanged(position, "favorite_update")
        }
    }

    fun refreshDownloadStatus(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, surahId)

                withContext(Dispatchers.Main) {
                    downloadStatusCache[surahId] = isDownloaded

                    val position = currentList.indexOfFirst { it.SoraId == surahId }
                    if (position != -1) {
                        notifyItemChanged(position, "download_update")
                    }
                }
            } catch (e: Exception) {

            }
        }
    }

    fun refreshAllDownloadStatuses() {
        lifecycleScope.launch {
            val updatedStatuses = withContext(Dispatchers.IO) {
                loadDownloadStatusesBatch(originalData.map { it.SoraId })
            }

            withContext(Dispatchers.Main) {
                downloadStatusCache.putAll(updatedStatuses)
                notifyDataSetChanged()
            }
        }
    }

    inner class MyHolder(private val binding: SoraListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val soraName = binding.soraName
        val soraNumber = binding.soraNumber
        val fav = binding.fav
        val download = binding.download
        val root = binding.root

        fun bind(soraSong: SoraSong) {

            soraName.text = Constants.SORA_OF_QURAN[soraSong.SoraId]
            soraNumber.text = "${soraSong.SoraId}"

            updateFavoriteIcon(favoriteStatusCache[soraSong.SoraId] ?: soraSong.isVaForte)
            updateDownloadIcon(soraSong)

            setupClickListeners(soraSong)
        }

        fun bind(soraSong: SoraSong, payloads: List<Any>) {
            if (payloads.isEmpty()) {
                bind(soraSong)
                return
            }

            payloads.forEach { payload ->
                when (payload) {
                    "favorite_update" -> {
                        updateFavoriteIcon(favoriteStatusCache[soraSong.SoraId] ?: soraSong.isVaForte)
                    }
                    "download_update" -> {
                        updateDownloadIcon(soraSong)
                    }
                    "progress_update" -> {
                        updateDownloadIcon(soraSong)
                    }
                }
            }
        }

        private fun updateFavoriteIcon(isFavorite: Boolean) {
            val iconRes = if (isFavorite) {
                R.drawable.ic_favorite_red_24
            } else {
                R.drawable.ic_baseline_favorite_border_24
            }
            fav.setImageResource(iconRes)
        }

        private fun updateDownloadIcon(soraSong: SoraSong) {
            val downloadId = "${readerId}_${soraSong.SoraId}"
            val progress = downloadProgressCache[downloadId]
            val isDownloaded = downloadStatusCache[soraSong.SoraId] ?: false

            when {
                progress != null && progress < 100 -> {
                    download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                    download.alpha = 0.7f
                    download.clearColorFilter()
                }
                isDownloaded -> {
                    download.setImageResource(R.drawable.ic_baseline_download_done_24)
                    download.alpha = 1.0f
                    download.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                }
                else -> {
                    download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                    download.alpha = 1.0f
                    download.clearColorFilter()
                }
            }
        }

        private fun setupClickListeners(soraSong: SoraSong) {
            fav.setOnClickListener {
                onItemClickListener.onItemFavClick(soraSong, it)
            }

            root.setOnClickListener {
                onItemClickListener.onItemClick(soraSong, it, bindingAdapterPosition)
            }

            download.setOnClickListener {
                onItemClickListener.onItemLongClick(soraSong, it, bindingAdapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            SoraListenerItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isNotEmpty()) {
            holder.bind(getItem(position), payloads)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    private val searchFilter: Filter = object : Filter() {
        override fun performFiltering(constraint: CharSequence): FilterResults {
            val filterPattern = constraint.toString().lowercase(Locale.getDefault()).trim()

            val filteredList = if (filterPattern.isEmpty()) {
                originalData
            } else {
                originalData.filter { soraSong ->
                    Constants.SORA_OF_QURAN[soraSong.SoraId]
                        .lowercase(Locale.getDefault())
                        .contains(filterPattern)
                }
            }

            return FilterResults().apply {
                values = filteredList
                count = filteredList.size
            }
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(constraint: CharSequence, results: FilterResults) {
            currentFilter = constraint.toString()
            val filteredItems = results.values as? List<SoraSong> ?: emptyList()

            super@QuranListenerReaderAdapter.submitList(filteredItems)
        }
    }

    override fun getFilter(): Filter = searchFilter

    fun cleanup() {
        pendingUpdates.values.forEach { runnable ->
            uiHandler.removeCallbacks(runnable)
        }
        pendingUpdates.clear()
        downloadStatusCache.clear()
        downloadProgressCache.clear()
        favoriteStatusCache.clear()
    }
}
