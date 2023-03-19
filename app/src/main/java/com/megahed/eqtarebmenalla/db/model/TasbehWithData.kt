package com.megahed.eqtarebmenalla.db.model

import androidx.room.Embedded
import androidx.room.Relation
import com.google.errorprone.annotations.Keep

@Keep
data class TasbehWithData(
    @Embedded val tasbeh: Tasbeh,
    @Relation(
        parentColumn = "id",
        entityColumn = "tasbehId"
    )
    val tasbehData: List<TasbehData>
)
