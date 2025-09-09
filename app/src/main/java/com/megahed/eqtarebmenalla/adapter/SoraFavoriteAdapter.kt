package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.databinding.SoraFavItemBinding
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
        val sFav = soraFav.soraSongData.filter {
            it.isVaForte
        }

        if (sFav.isNotEmpty()) {
            holder.soraName.text = soraFav.quranListenerReader.name

            val quranListenerReaderAdapter = QuranListenerReaderAdapter(
                context,
                object : OnItemWithFavClickListener<SoraSong> {
                    override fun onItemClick(itemObject: SoraSong, view: View?, position: Int) {
                        onMyItemClickListener.onItemClickReader(
                            itemObject,
                            view,
                            soraFav.quranListenerReader.name
                        )
                    }

                    override fun onItemFavClick(itemObject: SoraSong, view: View?) {
                        onMyItemClickListener.onItemFavClick(itemObject, view)
                    }

                    override fun onItemLongClick(itemObject: SoraSong, view: View?, position: Int) {
                        handleDownloadAction(itemObject, soraFav.quranListenerReader.id, soraFav.quranListenerReader.name, holder)
                    }
                },
                offlineAudioManager,
                soraFav.quranListenerReader.id,
                lifecycleScope
            )

            val verticalLayoutManager =
                LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            holder.recyclerView.layoutManager = verticalLayoutManager
            holder.recyclerView.setHasFixedSize(true)
            holder.recyclerView.adapter = quranListenerReaderAdapter
            quranListenerReaderAdapter.setData(sFav)
        } else {
            holder.cardContainer.visibility = View.GONE
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

                refreshAdapterForReader(readerId, holder)
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
                    delay(1000) // Wait 1 second

                    val isDownloaded = offlineAudioManager.isSurahDownloaded(readerId, soraSong.SoraId)

                    if (isDownloaded) {
                        withContext(Dispatchers.Main) {
                            showSnackbar(holder.root, "تم تحميل ${Constants.SORA_OF_QURAN[soraSong.SoraId]} بنجاح")
                            refreshAdapterForReader(readerId, holder)
                        }
                        return@launch
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun refreshAdapterForReader(readerId: String, holder: MyHolder) {
        val adapter = holder.recyclerView.adapter as? QuranListenerReaderAdapter
        adapter?.refreshAllDownloadStatuses()
    }

    private fun showSnackbar(view: View, message: String) {
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }
}