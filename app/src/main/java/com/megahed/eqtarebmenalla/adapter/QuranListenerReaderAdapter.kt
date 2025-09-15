package com.megahed.eqtarebmenalla.adapter

import android.content.Context
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class QuranListenerReaderAdapter(
    private val context: Context,
    private val onMyItemClickListener: OnItemWithFavClickListener<SoraSong>,
    private val offlineAudioManager: OfflineAudioManager,
    private val readerId: String,
    private val lifecycleScope: LifecycleCoroutineScope
) : ListAdapter<SoraSongWithState, QuranListenerReaderAdapter.MyHolder>(SurahDiffCallback()), Filterable {

    private val downloadStatusMap = ConcurrentHashMap<Int, Boolean>()
    private val downloadProgressMap = ConcurrentHashMap<String, Int>()

    private var originalData = listOf<SoraSongWithState>()
    private var filteredData = listOf<SoraSongWithState>()
    private var currentFilter = ""

    fun setData(data: List<SoraSong>) {
        lifecycleScope.launch {
            val dataWithState = withContext(Dispatchers.IO) {
                data.map { soraSong ->
                    val isDownloaded = try {
                        offlineAudioManager.isSurahDownloaded(readerId, soraSong.SoraId)
                    } catch (_: Exception) {
                        false
                    }

                    val downloadId = "${readerId}_${soraSong.SoraId}"
                    val downloadProgress = downloadProgressMap[downloadId]

                    SoraSongWithState(soraSong, isDownloaded, downloadProgress)
                }
            }

            withContext(Dispatchers.Main) {
                originalData = dataWithState
                applyCurrentFilter()
            }
        }
    }

    private fun applyCurrentFilter() {
        val filtered = if (currentFilter.isEmpty()) {
            originalData
        } else {
            originalData.filter { item ->
                Constants.SORA_OF_QURAN[item.soraSong.SoraId]
                    .lowercase(Locale.getDefault())
                    .contains(currentFilter.lowercase(Locale.getDefault()).trim())
            }
        }

        filteredData = filtered
        submitList(filtered)
    }


    fun updateDownloadProgress(progressMap: Map<String, Int>) {
        downloadProgressMap.clear()
        downloadProgressMap.putAll(progressMap)
        refreshDataWithCurrentState()
    }

    private fun refreshDataWithCurrentState() {
        lifecycleScope.launch(Dispatchers.Main) {
            val updatedData = originalData.map { item ->
                val downloadId = "${readerId}_${item.soraSong.SoraId}"
                val isDownloaded = downloadStatusMap[item.soraSong.SoraId] ?: item.isDownloaded
                val downloadProgress = downloadProgressMap[downloadId]

                item.copy(
                    isDownloaded = isDownloaded,
                    downloadProgress = downloadProgress
                )
            }

            originalData = updatedData
            applyCurrentFilter()
        }
    }

    fun refreshDownloadStatus(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, surahId)

                withContext(Dispatchers.Main) {
                    downloadStatusMap[surahId] = isDownloaded
                    refreshDataWithCurrentState()
                }
            } catch (_: Exception) {
            }
        }
    }

    fun refreshAllDownloadStatuses() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val statusMap = mutableMapOf<Int, Boolean>()

                originalData.forEach { item ->
                    val isDownloaded = try {
                        offlineAudioManager.isSurahDownloaded(readerId, item.soraSong.SoraId)
                    } catch (_: Exception) {
                        false
                    }
                    statusMap[item.soraSong.SoraId] = isDownloaded
                }

                withContext(Dispatchers.Main) {
                    downloadStatusMap.clear()
                    downloadStatusMap.putAll(statusMap)
                    refreshDataWithCurrentState()
                }
            } catch (_: Exception) {
            }
        }
    }

    class MyHolder( binding: SoraListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val soraName = binding.soraName
        val soraNumber = binding.soraNumber
        val fav = binding.fav
        val download = binding.download
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(SoraListenerItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val item = getItem(position)
        val quranListener = item.soraSong

        holder.soraName.text = Constants.SORA_OF_QURAN[quranListener.SoraId]
        holder.soraNumber.text = "${quranListener.SoraId}"

        if (quranListener.isVaForte) {
            holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
        } else {
            holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }

        setupDownloadIcon(holder, item)

        holder.fav.setOnClickListener {
            onMyItemClickListener.onItemFavClick(quranListener, it)
        }

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(quranListener, it)
        }

        holder.download.setOnClickListener {
            onMyItemClickListener.onItemLongClick(quranListener, it)
        }
    }

    private fun setupDownloadIcon(holder: MyHolder, item: SoraSongWithState) {
        when {
            item.downloadProgress != null -> {
                holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                holder.download.alpha = 0.7f
                holder.download.clearColorFilter()
            }

            item.isDownloaded -> {
                holder.download.setImageResource(R.drawable.ic_baseline_download_done_24)
                holder.download.alpha = 1.0f
                holder.download.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
            }

            else -> {
                holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                holder.download.alpha = 1.0f
                holder.download.clearColorFilter()
            }
        }
    }

    private val searchFilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterPattern = charSequence.toString().lowercase(Locale.getDefault()).trim()

            val filteredList = if (filterPattern.isEmpty()) {
                originalData
            } else {
                originalData.filter { item ->
                    Constants.SORA_OF_QURAN[item.soraSong.SoraId]
                        .lowercase(Locale.getDefault())
                        .contains(filterPattern)
                }
            }

            val results = FilterResults()
            results.values = filteredList
            return results
        }

        @Suppress("UNCHECKED_CAST")
        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            currentFilter = charSequence.toString()
            filterResults.values?.let { results ->
                val filteredItems = results as List<SoraSongWithState>
                filteredData = filteredItems
                submitList(filteredItems)
            }
        }
    }

    override fun getFilter(): Filter = searchFilter
}