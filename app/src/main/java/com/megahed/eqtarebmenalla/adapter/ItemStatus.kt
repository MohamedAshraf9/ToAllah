package com.megahed.eqtarebmenalla.adapter

data class ItemStatus(
    val isDownloaded: Boolean = false,
    val isLoading: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis(),
)