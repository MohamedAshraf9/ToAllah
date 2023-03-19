package com.megahed.eqtarebmenalla.db.customModel

import com.google.errorprone.annotations.Keep
import java.util.*

@Keep
data class HoursDate(
    val count:Long,
    val date:Date
)
