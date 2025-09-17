package com.megahed.eqtarebmenalla.feature_data.presentation.ui.schedule

data class DownloadSession(
    val readerId: String,
    val surahId: Int,
    val startVerse: Int,
    val endVerse: Int,
    val sessionId: String = System.currentTimeMillis().toString()
)