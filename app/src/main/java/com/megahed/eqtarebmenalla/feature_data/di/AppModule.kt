package com.megahed.eqtarebmenalla.feature_data.di

import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.feature_data.data.remote.AzanApi
import com.megahed.eqtarebmenalla.feature_data.data.repository.AzanRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.domain.repository.AzanRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

//dependency injection

@Module
@InstallIn(SingletonComponent::class)
class AppModule {
    @Provides
    @Singleton
    fun provideApi():AzanApi{
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AzanApi::class.java)
    }

    @Provides
    @Singleton
    fun provideRepository(api: AzanApi): AzanRepository {
        return AzanRepositoryImp(api)
    }

}