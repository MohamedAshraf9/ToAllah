package com.megahed.eqtarebmenalla.db.customModel

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.db.model.SoraSong

@Keep
data class SorasFavOfReader(
    val readerName:String,
    val soras:List<SoraSong>
)
