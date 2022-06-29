package com.megahed.eqtarebmenalla.feature_data.presentation

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.*
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.Date

data class IslamicListState(
    val isLoading:Boolean=false,
    val islamicInfo: IslamicInfo = IslamicInfo(3243,"dsad",
        Data(
            Date(
                gregorian = Gregorian(designation = Designation(), weekday = Weekday(), month = Month()),
                hijri = Hijri(designation = Designation(), weekday = WeekdayX(), month = MonthX())
            ),
            Meta(method = Method(location = Location(), params = Params()), offset = Offset())
            , Timings())
    ),
    val error:String=""
)
