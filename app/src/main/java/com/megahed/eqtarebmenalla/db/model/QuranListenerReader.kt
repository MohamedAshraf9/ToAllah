package com.megahed.eqtarebmenalla.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class QuranListenerReader(
    @PrimaryKey
    val id: String,
    val count: String,
    val letter: String,
    val name: String,
    val rewaya: String,
    val server: String,
    val suras: String,
    var isVaForte:Boolean=false
)