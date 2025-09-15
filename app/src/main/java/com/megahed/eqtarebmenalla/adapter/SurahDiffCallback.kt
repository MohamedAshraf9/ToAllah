package com.megahed.eqtarebmenalla.adapter

import androidx.recyclerview.widget.DiffUtil

class SurahDiffCallback : DiffUtil.ItemCallback<SoraSongWithState>() {
    override fun areItemsTheSame(oldItem: SoraSongWithState, newItem: SoraSongWithState): Boolean {
        return oldItem.soraSong.SoraId == newItem.soraSong.SoraId
    }

    override fun areContentsTheSame(oldItem: SoraSongWithState, newItem: SoraSongWithState): Boolean {
        return oldItem == newItem
    }
}