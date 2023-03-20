package com.megahed.eqtarebmenalla.feature_data.domain.use_cases

import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.feature_data.data.quranImage.QuranImage
import com.megahed.eqtarebmenalla.feature_data.data.remote.dto.prayerApi.IslamicInfo
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.dto.QuranListen
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.verse.VerseReaders
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import com.megahed.eqtarebmenalla.feature_data.domain.repository.QuranListenerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

class QuranImageUsesCase @Inject constructor(
    private val quranListenerRepository: QuranListenerRepository
){

    operator fun invoke( surah:Int, read:Int):Flow<Resource<QuranImage>> = flow {
        try {
            emit(Resource.Loading())
            val quran=quranListenerRepository.getSoraImages(surah, read)
            emit(Resource.Success(quran))

        }
        catch (e:HttpException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }
        catch (e:IOException){
            emit(Resource.Error(App.getInstance().getString(R.string.error)))
        }

    }

}