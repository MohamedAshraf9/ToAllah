package com.megahed.eqtarebmenalla.adapter

import androidx.recyclerview.widget.DiffUtil
import com.megahed.eqtarebmenalla.db.model.SoraSong

class SurahDiffCallback : DiffUtil.ItemCallback<SoraSong>() {
    override fun areItemsTheSame(oldItem: SoraSong, newItem: SoraSong): Boolean {
        return oldItem.SoraId == newItem.SoraId && oldItem.readerId == newItem.readerId
    }

    override fun areContentsTheSame(oldItem: SoraSong, newItem: SoraSong): Boolean {
        return oldItem.SoraId == newItem.SoraId &&
                oldItem.readerId == newItem.readerId &&
                oldItem.url == newItem.url &&
                oldItem.isVaForte == newItem.isVaForte
    }

    override fun getChangePayload(oldItem: SoraSong, newItem: SoraSong): Any? {
        return when {
            oldItem.isVaForte != newItem.isVaForte -> "favorite_update"
            oldItem.url != newItem.url -> "download_update"
            else -> null
        }
    }
}