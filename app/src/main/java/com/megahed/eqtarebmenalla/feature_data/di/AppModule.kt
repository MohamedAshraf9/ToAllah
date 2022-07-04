package com.megahed.eqtarebmenalla.feature_data.di

import android.app.Application
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.MyDatabase
import com.megahed.eqtarebmenalla.db.repository.AyaRepository
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.db.repository.SoraRepository
import com.megahed.eqtarebmenalla.db.repositoryImp.AyaRepositoryImp
import com.megahed.eqtarebmenalla.db.repositoryImp.PrayerTimeRepositoryImp
import com.megahed.eqtarebmenalla.db.repositoryImp.SoraRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.AllQuran
import com.megahed.eqtarebmenalla.feature_data.data.remote.IslamicApi
import com.megahed.eqtarebmenalla.feature_data.data.repository.IslamicRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
    fun provideDatabase(app: Application,trainDBLazy: Lazy<MyDatabase>): MyDatabase {
        return Room.databaseBuilder(
            app,
            MyDatabase::class.java,
            MyDatabase.DATABASE_NAME)
            .addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                /*
                WHAT GOES HERE?
                */
                CoroutineScope(Dispatchers.IO).launch {

                    val fileInString: String =
                        App.getInstance().assets.open("quran.json").bufferedReader().use { it.readText() }
                    val data= Gson().fromJson(fileInString,AllQuran::class.java)
                    for (i in 0 until data.surahs.size){
                        Log.d("MyTagData", data.surahs[i].name)
                    }


                }

            }
        }).build()

    }

    @Provides
    @Singleton
    fun providePrayerTimeRepository(db: MyDatabase): PrayerTimeRepository {
        return PrayerTimeRepositoryImp(db.prayerTime)
    }

    @Provides
    @Singleton
    fun provideSoraRepository(db: MyDatabase): SoraRepository {
        return SoraRepositoryImp(db.soraDao)
    }

    @Provides
    @Singleton
    fun provideAyaRepository(db: MyDatabase): AyaRepository {
        return AyaRepositoryImp(db.ayaDao)
    }


}