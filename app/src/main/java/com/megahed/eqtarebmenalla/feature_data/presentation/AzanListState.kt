package com.megahed.eqtarebmenalla.feature_data.presentation

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.AzanInfoDto

data class AzanListState(
    val isLoading:Boolean=false,
    val azanInfoDto: List<AzanInfoDto> = emptyList(),
    val error:String=""
)
