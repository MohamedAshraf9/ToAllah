package com.megahed.eqtarebmenalla.adapter

import android.annotation.SuppressLint
import androidx.recyclerview.widget.DiffUtil
import com.megahed.eqtarebmenalla.db.model.ReaderWithSora

class ReaderDiffCallback : DiffUtil.ItemCallback<ReaderWithSora>() {
    override fun areItemsTheSame(oldItem: ReaderWithSora, newItem: ReaderWithSora): Boolean {
        return oldItem.quranListenerReader.id == newItem.quranListenerReader.id
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: ReaderWithSora, newItem: ReaderWithSora): Boolean {
        return oldItem.quranListenerReader == newItem.quranListenerReader &&
                oldItem.soraSongData.map { "${it.SoraId}_${it.isVaForte}" } == newItem.soraSongData.map { "${it.SoraId}_${it.isVaForte}" }
    }
}