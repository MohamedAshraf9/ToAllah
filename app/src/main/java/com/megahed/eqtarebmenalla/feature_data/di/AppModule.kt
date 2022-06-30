package com.megahed.eqtarebmenalla.feature_data.di

import android.app.Application
import androidx.room.Room
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.MyDatabase
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repositoryImp.PrayerTimeRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.data.remote.IslamicApi
import com.megahed.eqtarebmenalla.feature_data.data.repository.IslamicRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIslamicApi():IslamicApi{
        return Retrofit.Builder()
            .baseUrl(Constants.BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IslamicApi::class.java)
    }

    @Provides
    @Singleton
    fun provideIslamicRepository(api: IslamicApi): IslamicRepository {
        return IslamicRepositoryImp(api)
    }


    @Provides
    @Singleton
    fun provideDatabase(app: Application): MyDatabase {
        return Room.databaseBuilder(
            app,
            MyDatabase::class.java,
            MyDatabase.DATABASE_NAME
        ).build()

    }

    @Provides
    @Singleton
    fun providePrayerTimeRepository(db: MyDatabase): PrayerTimeRepository {
        return PrayerTimeRepositoryImp(db.prayerTime)
    }


}