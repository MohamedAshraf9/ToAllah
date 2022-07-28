package com.megahed.eqtarebmenalla.db.customModel

import com.megahed.eqtarebmenalla.db.model.SoraSong

data class SorasFavOfReader(
    val readerName:String,
    val soras:List<SoraSong>
)
