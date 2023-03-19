package com.megahed.eqtarebmenalla.feature_data.data.local.dto.azkar

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.db.model.ElZekr
@Keep
data class AzkarItem(
    val category: String,
    val count: String,
    val description: String,
    val reference: String,
    val zekr: String
)
fun AzkarItem.toElZekr(catId:Int):ElZekr{
    return ElZekr(
        count = count,
        description = description,
        reference=reference,
        zekr=zekr,
        catId = catId
    )
}