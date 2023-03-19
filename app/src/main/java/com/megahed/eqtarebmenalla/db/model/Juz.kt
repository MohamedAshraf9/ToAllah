package com.megahed.eqtarebmenalla.db.model

import com.google.errorprone.annotations.Keep

@Keep
data class Juz(
    var index:Int,
    var startPosition:ArrayList<AyaPositions>,
    var endPosition:ArrayList<AyaPositions>
){

}
