package com.megahed.eqtarebmenalla.feature_data.states

import com.google.errorprone.annotations.Keep
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImageItem
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.RecitersVerse

@Keep
data class QuranImageState(
    val isLoading:Boolean=false,
    val quranImage: List<QuranImageItem> = emptyList(),
    val error:String=""
)

/*

Data(
Date(
gregorian = Gregorian(designation = Designation(), weekday = Weekday(), month = Month()),
hijri = Hijri(designation = Designation(), weekday = WeekdayX(), month = MonthX())
),
Meta(method = Method(location = Location(), params = Params()), offset = Offset())
, Timings())
*/
