package com.megahed.eqtarebmenalla.feature_data.data.remote.qibla.entity

import com.google.errorprone.annotations.Keep

@Keep
data class LocationLocal(
    val ref_country_codes: List<RefCountryCode>
)