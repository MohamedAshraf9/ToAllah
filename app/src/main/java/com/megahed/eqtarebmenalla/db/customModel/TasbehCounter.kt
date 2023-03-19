package com.megahed.eqtarebmenalla.db.customModel

import com.google.errorprone.annotations.Keep

@Keep
data class TasbehCounter(
    val tasbehName:String,
    val count:Long
)
