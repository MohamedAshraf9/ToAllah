package com.megahed.eqtarebmenalla.feature_data.domain.use_cases

import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import retrofit2.http.Query
import java.io.IOException
import javax.inject.Inject

class PrayerDataUsesCase @Inject constructor(
    private val repository: IslamicRepository
){

    //todo change AzanInfoDto to AzanInfo
    operator fun invoke(@Query("latitude") latitude:Double,
                        @Query("longitude") longitude:Double):Flow<Resource<IslamicInfo>> = flow {

        try {

            emit(Resource.Loading())
            val azanData=repository.getAzanData(longitude=longitude, latitude = latitude)
            emit(Resource.Success(azanData))

        }
        catch (e:HttpException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }
        catch (e:IOException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }

    }

}