package com.megahed.eqtarebmenalla.feature_data.presentation

import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter

data class QuranListenerListState(
    val isLoading:Boolean=false,
    val reciter: List<Reciter> = emptyList(),
    val error:String=""
)
