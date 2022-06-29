package com.megahed.eqtarebmenalla.feature_data.data.remote.dto

data class Method(
    val id: Int=0,
    val location: Location,
    val name: String="",
    val params: Params
)