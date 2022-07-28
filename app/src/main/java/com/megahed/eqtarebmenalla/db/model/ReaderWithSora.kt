package com.megahed.eqtarebmenalla.db.model

import androidx.room.Embedded
import androidx.room.Relation

data class ReaderWithSora(
    @Embedded val quranListenerReader: QuranListenerReader,
    @Relation(
        parentColumn = "id",
        entityColumn = "readerId"
    )
    val soraSongData: List<SoraSong>
)
