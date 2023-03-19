package com.megahed.eqtarebmenalla.feature_data.presentation

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.Reciter

@Keep
data class QuranListenerListState(
    val isLoading:Boolean=false,
    val reciter: List<Reciter> = emptyList(),
    val error:String=""
)
