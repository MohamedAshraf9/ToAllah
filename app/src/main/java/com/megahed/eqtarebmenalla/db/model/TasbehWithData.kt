package com.megahed.eqtarebmenalla.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class TasbehWithData(
    @Embedded val tasbeh: Tasbeh,
    @Relation(
        parentColumn = "id",
        entityColumn = "tasbehId"
    )
    val tasbehData: List<TasbehData>
)
