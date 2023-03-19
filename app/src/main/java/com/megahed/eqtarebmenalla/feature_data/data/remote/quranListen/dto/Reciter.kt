package com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
@Keep
data class Reciter(
    val count: String,
    val id: String,
    val letter: String,
    val name: String,
    val rewaya: String,
    val server: String,
    val suras: String
)
fun Reciter.toQuranListenerReader(): QuranListenerReader {
  return  QuranListenerReader(
      id=id,
      count=count,
      letter=letter,
      name=name,
      rewaya=rewaya,
      server=server,
      suras=suras
  )
}