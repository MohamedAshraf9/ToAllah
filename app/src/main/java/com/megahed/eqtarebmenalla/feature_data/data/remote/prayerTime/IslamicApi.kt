package com.megahed.eqtarebmenalla.feature_data.data.remote.prayerTime

import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IslamicApi {

    @GET("v1/timings")
    suspend fun getIslamicData(
        @Query("latitude") latitude:Double,
        @Query("longitude") longitude:Double,
        @Query("method") method:Int=5,

    ): IslamicInfo
}

/*

method
 0 - Shia Ithna-Ansari
1 - University of Islamic Sciences, Karachi
2 - Islamic Society of North America
3 - Muslim World League
4 - Umm Al-Qura University, Makkah
5 - Egyptian General Authority of Survey
7 - Institute of Geophysics, University of Tehran
8 - Gulf Region
9 - Kuwait
10 - Qatar
11 - Majlis Ugama Islam Singapura, Singapore
12 - Union Organization islamic de France
13 - Diyanet İşleri Başkanlığı, Turkey
14 - Spiritual Administration of Muslims of Russia
15 - Moonsighting Committee Worldwide (also requires shafaq parameter)
16 - Dubai (unofficial)

 */