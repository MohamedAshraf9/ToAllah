package com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi

import com.google.errorprone.annotations.Keep

@Keep
data class Hijri(
    val date: String="",
    val day: String="",
    val designation: Designation,
    val format: String="",
    val holidays: List<Any> = emptyList(),
    val month: MonthX,
    val weekday: WeekdayX,
    val year: String=""
)