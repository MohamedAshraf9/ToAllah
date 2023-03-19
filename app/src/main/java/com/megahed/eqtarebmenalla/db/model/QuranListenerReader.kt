package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.errorprone.annotations.Keep

@Entity
@Keep
data class QuranListenerReader(
    @PrimaryKey
    var id: String,
    var count: String,
    var letter: String,
    var name: String,
    var rewaya: String,
    var server: String,
    var suras: String,
    var isVaForte:Boolean=false
)