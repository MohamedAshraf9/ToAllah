package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
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
import com.megahed.eqtarebmenalla.myListener.OnItemWithFavClickListener
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SoraFavoriteAdapter(
    private val context: Context,
    private val onMyItemClickListener: OnItemReaderClickListener<SoraSong>,
    private val offlineAudioManager: OfflineAudioManager,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<SoraFavoriteAdapter.MyHolder>() {

    private var listData = mutableListOf<ReaderWithSora>()

    @SuppressLint("NotifyDataSetChanged")
    fun setData(data: List<ReaderWithSora>) {
        listData.clear()
        listData.addAll(data)
        notifyDataSetChanged()
    }

    class MyHolder(binding: SoraFavItemBinding) : RecyclerView.ViewHolder(binding.root) {
        val soraName = binding.readerName
        val recyclerView = binding.recyclerView
        val cardContainer = binding.cardContainer
        val root = binding.root
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyHolder {
        return MyHolder(SoraFavItemBinding.inflate(LayoutInflater.from(context), parent, false))
    }

    override fun onBindViewHolder(holder: MyHolder, position: Int) {
        val soraFav = listData[position]
        val sFav = soraFav.soraSongData.filter { it.isVaForte }

        if (sFav.isNotEmpty()) {
            holder.cardContainer.visibility = View.VISIBLE
            holder.soraName.text = soraFav.quranListenerReader.name

            val innerAdapter = InnerAdapter(sFav, soraFav, holder)

            val verticalLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            holder.recyclerView.layoutManager = verticalLayoutManager
            holder.recyclerView.setHasFixedSize(true)
            holder.recyclerView.adapter = innerAdapter
        } else {
            holder.cardContainer.visibility = View.GONE
        }
    }
    inner class InnerAdapter(
        private var items: List<SoraSong>,
        private val soraFav: ReaderWithSora,
        private val parentHolder: MyHolder
    ) : RecyclerView.Adapter<InnerAdapter.InnerViewHolder>() {

        inner class InnerViewHolder(binding: SoraListenerItemBinding) :
            RecyclerView.ViewHolder(binding.root) {
            val soraName = binding.soraName
            val soraNumber = binding.soraNumber
            val fav = binding.fav
            val download = binding.download
            val root = binding.root
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerViewHolder {
            val binding =
                SoraListenerItemBinding.inflate(LayoutInflater.from(context), parent, false)
            return InnerViewHolder(binding)
        }

        override fun onBindViewHolder(holder: InnerViewHolder, position: Int) {
            val item = items[position]

            holder.soraName.text = Constants.SORA_OF_QURAN[item.SoraId]
            holder.soraNumber.text = "${item.SoraId}"

            if (item.isVaForte) {
                holder.fav.setImageResource(R.drawable.ic_favorite_red_24)
            } else {
                holder.fav.setImageResource(R.drawable.ic_baseline_favorite_border_24)
            }
            setupDownloadIcon(holder, item, soraFav.quranListenerReader.id)

            holder.fav.setOnClickListener {
                onMyItemClickListener.onItemFavClick(item, it)
            }

            holder.root.setOnClickListener {
                onMyItemClickListener.onItemClickReader(item, it, soraFav.quranListenerReader.name)
            }

            holder.download.setOnClickListener {
                handleDownloadAction(
                    item,
                    soraFav.quranListenerReader.id,
                    soraFav.quranListenerReader.name,
                    parentHolder
                )
            }
            lifecycleScope.launch {
                updateDownloadIcon(holder, item, soraFav.quranListenerReader.id)
            }
        }

        fun updateItemDownloadStatus(soraId: Int) {
            val position = items.indexOfFirst { it.SoraId == soraId }
            if (position != -1) {
                notifyItemChanged(position)
            }
        }

        private suspend fun updateDownloadIcon(
            holder: InnerViewHolder,
            item: SoraSong,
            readerId: String
        ) {
            try {
                val isDownloaded = withContext(Dispatchers.IO) {
                    offlineAudioManager.isSurahDownloaded(readerId, item.SoraId)
                }

                withContext(Dispatchers.Main) {
                    if (isDownloaded) {
                        holder.download.setImageResource(R.drawable.ic_baseline_download_done_24)
                        holder.download.setColorFilter(
                            ContextCompat.getColor(
                                context,
                                R.color.success_green
                            )
                        )
                    } else {
                        holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                        holder.download.clearColorFilter()
                    }
                    holder.download.alpha = 1.0f
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                    holder.download.alpha = 1.0f
                    holder.download.clearColorFilter()
                }
            }
        }

        override fun getItemCount(): Int = items.size

    }


    private fun setupDownloadIcon(holder: InnerAdapter.InnerViewHolder, item: SoraSong, readerId: String) {
        lifecycleScope.launch {
            try {
                val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, item.SoraId)

                if (isDownloaded) {
                    holder.download.setImageResource(R.drawable.ic_baseline_download_done_24)
                    holder.download.alpha = 1.0f
                    holder.download.setColorFilter(ContextCompat.getColor(context, R.color.success_green))
                } else {
                    holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                    holder.download.alpha = 1.0f
                    holder.download.clearColorFilter()
                }
            } catch (e: Exception) {
                holder.download.setImageResource(R.drawable.ic_baseline_arrow_circle_down_24)
                holder.download.alpha = 1.0f
                holder.download.clearColorFilter()
            }
        }
    }

    override fun getItemCount(): Int {
        return listData.size
    }

    fun getAllFavSongs(): List<SoraSong> {
        return listData.flatMap { it.soraSongData.filter { sora -> sora.isVaForte } }
    }


    private fun handleDownloadAction(
        soraSong: SoraSong,
        readerId: String,
        readerName: String,
        holder: MyHolder
    ) {
        lifecycleScope.launch {
            try {
                val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, soraSong.SoraId)

                if (isDownloaded) {
                    showDeleteDialog(soraSong, readerId, readerName, holder)
                } else {
                    downloadSura(soraSong, readerId, readerName, holder)
                }
            } catch (e: Exception) {
                showSnackbar(holder.root, "حدث خطأ في التحقق من حالة التحميل")
            }
        }
    }

    private fun showDeleteDialog(
        soraSong: SoraSong,
        readerId: String,
        readerName: String,
        holder: MyHolder
    ) {
        MaterialAlertDialogBuilder(context)
            .setTitle("حذف الملف المحمل")
            .setMessage("هل تريد حذف ${Constants.SORA_OF_QURAN[soraSong.SoraId]} المحملة؟")
            .setPositiveButton("حذف") { _, _ ->
                deleteSura(soraSong, readerId, holder)
            }
            .setNegativeButton("إلغاء", null)
            .show()
    }

    private fun downloadSura(
        soraSong: SoraSong,
        readerId: String,
        readerName: String,
        holder: MyHolder
    ) {
        lifecycleScope.launch {
            try {
                val success = offlineAudioManager.downloadSurahAudio(
                    readerId = readerId,
                    surahId = soraSong.SoraId,
                    surahName = Constants.SORA_OF_QURAN[soraSong.SoraId] ?: "سورة ${soraSong.SoraId}",
                    readerName = readerName,
                    audioUrl = soraSong.url
                )

                if (success) {
                    showSnackbar(holder.root, "بدأ تحميل ${Constants.SORA_OF_QURAN[soraSong.SoraId]}...")

                    monitorDownloadCompletion(soraSong, readerId, holder)
                } else {
                    showSnackbar(holder.root, "فشل في بدء التحميل")
                }
            } catch (e: Exception) {
                showSnackbar(holder.root, "حدث خطأ في التحميل")
            }
        }
    }

    private fun deleteSura(soraSong: SoraSong, readerId: String, holder: MyHolder) {
        lifecycleScope.launch {
            try {
                offlineAudioManager.deleteDownloadedAudio(readerId, soraSong.SoraId)
                showSnackbar(holder.root, "تم حذف ${Constants.SORA_OF_QURAN[soraSong.SoraId]}")

                (holder.recyclerView.adapter as? InnerAdapter)?.updateItemDownloadStatus(soraSong.SoraId)
            } catch (e: Exception) {
                showSnackbar(holder.root, "حدث خطأ في حذف الملف")
            }
        }
    }

    private fun monitorDownloadCompletion(
        soraSong: SoraSong,
        readerId: String,
        holder: MyHolder
    ) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                repeat(30) {
                    delay(1000)

                    val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, soraSong.SoraId)

                    if (isDownloaded) {
                        withContext(Dispatchers.Main) {
                            showSnackbar(holder.root, "تم تحميل ${Constants.SORA_OF_QURAN[soraSong.SoraId]} بنجاح")
                            (holder.recyclerView.adapter as? InnerAdapter)?.updateItemDownloadStatus(soraSong.SoraId)
                        }
                        return@launch
                    }
                }
            } catch (_: Exception) {
            }
        }
    }



    private fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}