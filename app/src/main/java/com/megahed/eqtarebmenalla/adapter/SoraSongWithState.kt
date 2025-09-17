package com.megahed.eqtarebmenalla.adapter

import com.megahed.eqtarebmenalla.db.model.SoraSong

data class SoraSongWithState(
    val soraSong: SoraSong,
    val isDownloaded: Boolean = false,
    val downloadProgress: Int? = null
) {
    val id: String = "${soraSong.SoraId}_${soraSong.isVaForte}_${isDownloaded}_${downloadProgress}"
}