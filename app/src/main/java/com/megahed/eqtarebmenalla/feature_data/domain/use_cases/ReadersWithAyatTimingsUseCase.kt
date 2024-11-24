package com.megahed.eqtarebmenalla.feature_data.domain.use_cases

import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.feature_data.domain.repository.QuranListenerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class ReadersWithAyatTimingsUseCase @Inject constructor(
    private val quranListenerRepository: QuranListenerRepository
){

    operator fun invoke(): Flow<Resource<List<QuranListenerReader>>> = flow {
        try {
            emit(Resource.Loading())
            val quranListenerReaders = quranListenerRepository.getReadersWithAyatTimings()
            emit(Resource.Success(quranListenerReaders))

        }
        catch (e: HttpException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }
        catch (e: IOException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }

    }

}