package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Filter
import android.widget.Filterable
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
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

class QuranListenerReaderAdapter(
    private val context: Context,
    private val onMyItemClickListener: OnItemWithFavClickListener<SoraSong>,
    private val offlineAudioManager: OfflineAudioManager,
    private val readerId: String,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<QuranListenerReaderAdapter.MyHolder>(), Filterable {

    private var listData = mutableListOf<SoraSong>()
    private var listDataSearch = mutableListOf<SoraSong>()

    private val downloadStatusMap = mutableMapOf<Int, Boolean>()
    private val downloadProgressMap = mutableMapOf<String, Int>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<SoraSong>) {
        listData.clear()
        listData.addAll(data)
        listDataSearch.clear()
        listDataSearch.addAll(data)
        notifyDataSetChanged()

        checkDownloadStatusForAll()
    }

    fun updateDownloadStatus(statusMap: Map<Int, Boolean>) {
        downloadStatusMap.clear()
        downloadStatusMap.putAll(statusMap)
        notifyItemRangeChanged(0, listData.size)
    }

    fun updateDownloadProgress(progressMap: Map<String, Int>) {
        downloadProgressMap.clear()
        downloadProgressMap.putAll(progressMap)
        notifyItemRangeChanged(0, listData.size)
    }

    private fun checkDownloadStatusForAll() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val dataCopy = listData.toList()
                val statusMap = mutableMapOf<Int, Boolean>()

                dataCopy.forEach { soraSong ->
                    val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, soraSong.SoraId)
                    statusMap[soraSong.SoraId] = isDownloaded
                }

                withContext(Dispatchers.Main) {
                    downloadStatusMap.clear()
                    downloadStatusMap.putAll(statusMap)
                    notifyItemRangeChanged(0, listData.size)
                }
            } catch (e: Exception) {
            }
        }
    }

    fun refreshDownloadStatus(surahId: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, surahId)

                withContext(Dispatchers.Main) {
                    downloadStatusMap[surahId] = isDownloaded
                    val position = listData.indexOfFirst { it.SoraId == surahId }
                    if (position >= 0) {
                        notifyItemChanged(position)
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    fun refreshAllDownloadStatuses() {
        checkDownloadStatusForAll()
    }

    class MyHolder(binding: SoraListenerItemBinding) : RecyclerView.ViewHolder(binding.root) {
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
        val quranListener = listData[position]
        holder.soraName.text = Constants.SORA_OF_QURAN[quranListener.SoraId]
        holder.soraNumber.text = "${quranListener.SoraId}"

        if (quranListener.isVaForte) {
            holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
        } else {
            holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
        }

        setupDownloadIcon(holder, quranListener)

        holder.fav.setOnClickListener {
            onMyItemClickListener.onItemFavClick(listData[position], it)
        }

        holder.itemView.setOnClickListener {
            onMyItemClickListener.onItemClick(listData[position], it)
        }

        holder.download.setOnClickListener {
            onMyItemClickListener.onItemLongClick(listData[position], it)
        }
    }

    private fun setupDownloadIcon(holder: MyHolder, soraSong: SoraSong) {
        val downloadId = "${readerId}_${soraSong.SoraId}"
        val isDownloaded = downloadStatusMap[soraSong.SoraId] == true
        val downloadProgress = downloadProgressMap[downloadId]

        when {
            downloadProgress != null -> {
                holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                holder.download.alpha = 0.7f
                holder.download.clearColorFilter()
                Log.d("AdapterDownload", "Surah ${soraSong.SoraId} downloading: $downloadProgress%")
            }

            isDownloaded -> {
                holder.download.setImageResource(R.drawable.ic_baseline_download_done_24)
                holder.download.alpha = 1.0f
                holder.download.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                Log.d("AdapterDownload", "Surah ${soraSong.SoraId} is downloaded")
            }

            else -> {
                holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                holder.download.alpha = 1.0f
                holder.download.clearColorFilter()
                Log.d("AdapterDownload", "Surah ${soraSong.SoraId} not downloaded")
            }
        }
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    private val examplefilter: Filter = object : Filter() {
        override fun performFiltering(charSequence: CharSequence): FilterResults {
            val filterlist = mutableListOf<SoraSong>()
            if (charSequence.isEmpty()) {
                filterlist.addAll(listDataSearch)
            } else {
                val filterPattern = charSequence.toString().lowercase(Locale.getDefault()).trim { it <= ' ' }
                val searchDataCopy = listDataSearch.toList()
                for (item in searchDataCopy) {
                    if (Constants.SORA_OF_QURAN[item.SoraId].lowercase().contains(filterPattern)) {
                        filterlist.add(item)
                    }
                }
            }
            val results = FilterResults()
            results.values = filterlist
            return results
        }

        @SuppressLint("NotifyDataSetChanged")
        override fun publishResults(charSequence: CharSequence, filterResults: FilterResults) {
            if (filterResults.values != null) {
                listData.clear()
                listData.addAll(filterResults.values as MutableList<SoraSong>)
                notifyDataSetChanged()
                checkDownloadStatusForAll()
            }
        }
    }

    override fun getFilter(): Filter {
        return examplefilter
    }
}