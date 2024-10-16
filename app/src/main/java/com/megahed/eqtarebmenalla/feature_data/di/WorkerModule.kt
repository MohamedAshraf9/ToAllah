package com.megahed.eqtarebmenalla.feature_data.di

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideWorkManagerConfiguration(workerFactory: HiltWorkerFactory): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }
}