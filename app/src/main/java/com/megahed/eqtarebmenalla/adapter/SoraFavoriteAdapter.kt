package com.megahed.eqtarebmenalla.adapter

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.SoraFavItemBinding
import com.megahed.eqtarebmenalla.databinding.SoraListenerItemBinding
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.myListener.OnItemReaderClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class SoraFavoriteAdapter(
    private val context: Context,
    private val onMyItemClickListener: OnItemReaderClickListener<SoraSong>,
    private val offlineAudioManager: OfflineAudioManager,
    private val lifecycleScope: LifecycleCoroutineScope,
) : ListAdapter<ReaderWithSora, SoraFavoriteAdapter.MyHolder>(ReaderDiffCallback()) {

    private val downloadStatusCache = ConcurrentHashMap<String, Boolean>()
    private val favoriteItemsCache = ConcurrentHashMap<String, List<SoraSong>>()

    private val uiUpdateHandler = Handler(Looper.getMainLooper())
    private val pendingUpdates = ConcurrentHashMap<String, Runnable>()

    inner class MyHolder(private val binding: SoraFavItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val soraName = binding.readerName
        val recyclerView = binding.recyclerView
        val cardContainer = binding.cardContainer
        val root = binding.root

        private var innerAdapter: InnerAdapter? = null

        fun bind(readerWithSora: ReaderWithSora) {

            val favoriteItems = getFavoriteItems(readerWithSora)

            if (favoriteItems.isNotEmpty()) {
                cardContainer.visibility = View.VISIBLE
                soraName.text = readerWithSora.quranListenerReader.name

                setupRecyclerView(favoriteItems, readerWithSora)
            } else {
                cardContainer.visibility = View.GONE
                innerAdapter = null
            }
        }

        private fun getFavoriteItems(readerWithSora: ReaderWithSora): List<SoraSong> {
            val cacheKey = readerWithSora.quranListenerReader.id
            return favoriteItemsCache.getOrPut(cacheKey) {
                readerWithSora.soraSongData.filter { it.isVaForte }
            }
        }

        private fun setupRecyclerView(
            favoriteItems: List<SoraSong>,
            readerWithSora: ReaderWithSora,
        ) {
            if (innerAdapter == null) {
                innerAdapter = InnerAdapter(this@SoraFavoriteAdapter, readerWithSora, this)

                val layoutManager =
                    LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                recyclerView.apply {
                    this.layoutManager = layoutManager
                    setHasFixedSize(true)
                    adapter = innerAdapter

                    setItemViewCacheSize(20)
                    isNestedScrollingEnabled = false
                }
            }

            innerAdapter?.updateItems(favoriteItems)
        }

        fun updateDownloadStatus(surahId: Int) {
            innerAdapter?.updateItemDownloadStatus(surahId)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(
            SoraFavItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateData(data: List<ReaderWithSora>) {
        favoriteItemsCache.clear()
        downloadStatusCache.clear()

        submitList(data)
    }

    fun getAllFavSongs(): List<SoraSong> {
        return currentList.flatMap { readerWithSora ->
            favoriteItemsCache[readerWithSora.quranListenerReader.id]
                ?: readerWithSora.soraSongData.filter { it.isVaForte }
        }
    }

    fun cleanup() {
        pendingUpdates.values.forEach { runnable ->
            uiUpdateHandler.removeCallbacks(runnable)
        }
        pendingUpdates.clear()
        downloadStatusCache.clear()
        favoriteItemsCache.clear()
    }

    inner class InnerAdapter(
        private val parentAdapter: SoraFavoriteAdapter,
        private val readerWithSora: ReaderWithSora,
        private val parentHolder: MyHolder,
    ) : RecyclerView.Adapter<InnerAdapter.InnerViewHolder>() {

        private var items = listOf<SoraSong>()
        private val itemStatusCache = ConcurrentHashMap<Int, ItemStatus>()


        inner class InnerViewHolder(private val binding: SoraListenerItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val soraName = binding.soraName
            val soraNumber = binding.soraNumber
            val fav = binding.fav
            val download = binding.download
            val root = binding.root

            fun bind(soraSong: SoraSong) {

                soraName.text = Constants.SORA_OF_QURAN[soraSong.SoraId]
                soraNumber.text = "${soraSong.SoraId}"

                updateFavoriteIcon(soraSong.isVaForte)

                updateDownloadIconCached(soraSong)

                setupClickListeners(soraSong)
            }

            private fun updateFavoriteIcon(isFavorite: Boolean) {
                val iconRes = if (isFavorite) {
                    R.drawable.ic_favorite_red_24
                } else {
                    R.drawable.ic_baseline_favorite_border_24
                }
                fav.setImageResource(iconRes)
            }

            private fun updateDownloadIconCached(soraSong: SoraSong) {
                val status = itemStatusCache[soraSong.SoraId]
                val cacheKey = "${readerWithSora.quranListenerReader.id}_${soraSong.SoraId}"

                if (status != null && (System.currentTimeMillis() - status.lastUpdated) < 30000) {
                    applyDownloadIconState(status)
                } else {

                    lifecycleScope.launch {
                        loadAndUpdateDownloadStatus(soraSong, cacheKey)
                    }
                }
            }

            private suspend fun loadAndUpdateDownloadStatus(soraSong: SoraSong, cacheKey: String) {
                try {
                    val isDownloaded = downloadStatusCache[cacheKey] ?: run {
                        val status = withContext(Dispatchers.IO) {
                            offlineAudioManager.isSurahDownloaded(
                                readerWithSora.quranListenerReader.id,
                                soraSong.SoraId
                            )
                        }
                        downloadStatusCache[cacheKey] = status
                        status
                    }

                    val itemStatus = ItemStatus(isDownloaded = isDownloaded)
                    itemStatusCache[soraSong.SoraId] = itemStatus

                    withContext(Dispatchers.Main) {
                        applyDownloadIconState(itemStatus)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        applyDownloadIconState(ItemStatus()) // Default state
                    }
                }
            }

            private fun applyDownloadIconState(status: ItemStatus) {
                when {
                    status.isLoading -> {
                        download.setImageResource(R.drawable.ic_baseline_hourglass_empty_24)
                        download.alpha = 0.7f
                        download.clearColorFilter()
                    }

                    status.isDownloaded -> {
                        download.setImageResource(R.drawable.ic_baseline_download_done_24)
                        download.alpha = 1.0f
                        download.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.success_green
                            )
                        )
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
                    onMyItemClickListener.onItemFavClick(soraSong, it)
                }

                root.setOnClickListener {
                    onMyItemClickListener.onItemClickReader(
                        soraSong,
                        it,
                        readerWithSora.quranListenerReader.name
                    )
                }

                download.setOnClickListener {
                    handleDownloadAction(soraSong)
                }
            }

            private fun handleDownloadAction(soraSong: SoraSong) {
                lifecycleScope.launch {
                    try {
                        // Show loading immediately
                        val currentStatus = itemStatusCache[soraSong.SoraId] ?: ItemStatus()
                        itemStatusCache[soraSong.SoraId] = currentStatus.copy(isLoading = true)
                        applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)

                        val isDownloaded = withContext(Dispatchers.IO) {
                            offlineAudioManager.isSurahDownloaded(
                                readerWithSora.quranListenerReader.id,
                                soraSong.SoraId
                            )
                        }

                        withContext(Dispatchers.Main) {

                            itemStatusCache[soraSong.SoraId] =
                                ItemStatus(isDownloaded = isDownloaded)
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)

                            if (isDownloaded) {
                                showDeleteDialog(soraSong)
                            } else {
                                downloadSura(soraSong)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            itemStatusCache[soraSong.SoraId] = ItemStatus()
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                            showErrorSnackbar(root, "حدث خطأ في التحقق من حالة التحميل")
                        }
                    }
                }
            }

            private fun showDeleteDialog(soraSong: SoraSong) {
                MaterialAlertDialogBuilder(context)
                    .setTitle("حذف الملف المحمل")
                    .setMessage("هل تريد حذف ${Constants.SORA_OF_QURAN[soraSong.SoraId]} المحملة؟")
                    .setPositiveButton("حذف") { _, _ ->
                        deleteSura(soraSong)
                    }
                    .setNegativeButton("إلغاء") { _, _ ->

                        val currentStatus = itemStatusCache[soraSong.SoraId] ?: ItemStatus()
                        itemStatusCache[soraSong.SoraId] = currentStatus.copy(isLoading = false)
                        applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                    }
                    .show()
            }

            private fun downloadSura(soraSong: SoraSong) {
                lifecycleScope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) {
                            offlineAudioManager.downloadSurahAudioOptimized(
                                readerId = readerWithSora.quranListenerReader.id,
                                surahId = soraSong.SoraId,
                                surahName = Constants.SORA_OF_QURAN[soraSong.SoraId],
                                readerName = readerWithSora.quranListenerReader.name,
                                audioUrl = soraSong.url
                            )
                        }

                        withContext(Dispatchers.Main) {
                            if (success) {
                                showSuccessSnackbar(
                                    root,
                                    "بدأ تحميل ${Constants.SORA_OF_QURAN[soraSong.SoraId]}..."
                                )

                                itemStatusCache[soraSong.SoraId] = ItemStatus(isLoading = true)
                                applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)

                                monitorDownloadCompletion(soraSong)
                            } else {
                                itemStatusCache[soraSong.SoraId] = ItemStatus()
                                applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                                showErrorSnackbar(root, "فشل في بدء التحميل")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            itemStatusCache[soraSong.SoraId] = ItemStatus()
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                            showErrorSnackbar(root, "حدث خطأ في التحميل")
                        }
                    }
                }
            }

            private fun deleteSura(soraSong: SoraSong) {
                lifecycleScope.launch {
                    try {
                        val success = withContext(Dispatchers.IO) {
                            offlineAudioManager.deleteDownloadedAudioOptimized(
                                readerWithSora.quranListenerReader.id,
                                soraSong.SoraId
                            )
                            true
                        }

                        withContext(Dispatchers.Main) {
                            if (success) {

                                val cacheKey =
                                    "${readerWithSora.quranListenerReader.id}_${soraSong.SoraId}"
                                downloadStatusCache[cacheKey] = false
                                itemStatusCache[soraSong.SoraId] = ItemStatus(isDownloaded = false)
                                applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)

                                showSuccessSnackbar(
                                    root,
                                    "تم حذف ${Constants.SORA_OF_QURAN[soraSong.SoraId]}"
                                )
                            } else {
                                itemStatusCache[soraSong.SoraId] = ItemStatus()
                                applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                                showErrorSnackbar(root, "حدث خطأ في حذف الملف")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            itemStatusCache[soraSong.SoraId] = ItemStatus()
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                            showErrorSnackbar(root, "حدث خطأ في حذف الملف")
                        }
                    }
                }
            }

            private fun monitorDownloadCompletion(soraSong: SoraSong) {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        repeat(30) {
                            delay(1000)

                            val isDownloaded = offlineAudioManager.isSurahDownloaded(
                                readerWithSora.quranListenerReader.id,
                                soraSong.SoraId
                            )

                            if (isDownloaded) {
                                withContext(Dispatchers.Main) {

                                    val cacheKey =
                                        "${readerWithSora.quranListenerReader.id}_${soraSong.SoraId}"
                                    downloadStatusCache[cacheKey] = true
                                    itemStatusCache[soraSong.SoraId] =
                                        ItemStatus(isDownloaded = true)
                                    applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)

                                    showSuccessSnackbar(
                                        root,
                                        "تم تحميل ${Constants.SORA_OF_QURAN[soraSong.SoraId]} بنجاح"
                                    )
                                }
                                return@launch
                            }
                        }

                        withContext(Dispatchers.Main) {
                            itemStatusCache[soraSong.SoraId] = ItemStatus()
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            itemStatusCache[soraSong.SoraId] = ItemStatus()
                            applyDownloadIconState(itemStatusCache[soraSong.SoraId]!!)
                        }
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
            return InnerViewHolder(
                SoraListenerItemBinding.inflate(
                    LayoutInflater.from(context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        fun updateItems(newItems: List<SoraSong>) {
            items = newItems
            notifyDataSetChanged()
        }

        fun updateItemDownloadStatus(soraId: Int) {
            val position = items.indexOfFirst { it.SoraId == soraId }
            if (position != -1) {

                itemStatusCache.remove(soraId)
                notifyItemChanged(position)
            }
        }
    }

    private fun showSuccessSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT)
            .setBackgroundTint(ContextCompat.getColor(context, R.color.success_green))
            .setTextColor(ContextCompat.getColor(context, android.R.color.white))
            .show()
    }

    private fun showErrorSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(context, R.color.error_red))
            .setTextColor(ContextCompat.getColor(context, android.R.color.white))
            .show()
    }
}