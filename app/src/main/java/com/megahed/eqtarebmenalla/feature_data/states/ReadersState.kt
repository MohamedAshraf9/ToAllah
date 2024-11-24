package com.megahed.eqtarebmenalla.feature_data.states

import androidx.annotation.Keep
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader

@Keep
data class ReadersState(
    val isLoading:Boolean=false,
    val readers: List<QuranListenerReader> = emptyList(),
    val error:String=""
)