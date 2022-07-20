package com.megahed.eqtarebmenalla.feature_data.di

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.MyDatabase
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.repository.*
import com.megahed.eqtarebmenalla.db.repositoryImp.*
import com.megahed.eqtarebmenalla.exoplayer.MusicServiceConnection
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.AllQuran
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.toAya
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.allQran.toSora
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.azkar.Azkar
import com.megahed.eqtarebmenalla.feature_data.data.local.dto.azkar.toElZekr
import com.megahed.eqtarebmenalla.feature_data.data.remote.prayerTime.IslamicApi
import com.megahed.eqtarebmenalla.feature_data.data.remote.quranListen.QuranListenApi
import com.megahed.eqtarebmenalla.feature_data.data.repository.IslamicRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.data.repository.QuranListenerRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import com.megahed.eqtarebmenalla.feature_data.domain.repository.QuranListenerRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideIslamicApi(): IslamicApi {
        return Retrofit.Builder()
            .baseUrl(Constants.PRAYER_TIME_BASE_URL)
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
    fun provideQuranApi(): QuranListenApi {
        return Retrofit.Builder()
            .baseUrl(Constants.QURAN_LISTEN_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(QuranListenApi::class.java)
    }

    @Provides
    @Singleton
    fun provideQuranListenerRepository(api: QuranListenApi): QuranListenerRepository {
        return QuranListenerRepositoryImp(api)
    }


    @Provides
    @Singleton
    fun provideDatabase(app: Application,trainDBLazy: Provider<MyDatabase>): MyDatabase {
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
                        trainDBLazy.get().soraDao.insertSora(
                           data.surahs[i].toSora(data.surahs[i].ayahs.size)
                        )

                        //Log.d("MyTagData ", "loop = $i")

                        //Log.d("MyTagData ", data.surahs[i].name)
                        //Log.d("MyTagData ", "=============================================")
                        for (j in 0 until data.surahs[i].ayahs.size){
                           // Log.d("MyTagData ", data.surahs[i].ayahs[j].text)
                            trainDBLazy.get().ayaDao.insertAya(
                                data.surahs[i].ayahs[j].toAya(i+1)
                            )
                        }
                    }


                }
                CoroutineScope(Dispatchers.IO).launch {
                    val fileInString: String =
                        App.getInstance().assets.open("azkar.json").bufferedReader().use { it.readText() }
                    val data= Gson().fromJson(fileInString,Azkar::class.java)
                    var id=0
                    for (i in 0 until data.size){
                       val d= trainDBLazy.get().azkarCategoryDao.insertAzkarCategory(
                            AzkarCategory(data[i].category)
                        )
                        if (d>0&&d.toInt()!=id){
                            id=d.toInt()
                        }
                        Log.d("MyTagData ", "d = $d")
                        Log.d("MyTagData ", "id = $id")
                        trainDBLazy.get().elZekrDao.insertElZekr(
                            data[i].toElZekr(id)
                        )

                    }

                }
                CoroutineScope(Dispatchers.IO).launch {
                    val tasbeh=App.getInstance().resources.getStringArray(R.array.tasbeh)
                    for (i in tasbeh.indices){
                        trainDBLazy.get().tasbehDao.insertTasbeh(
                            Tasbeh(tasbeh[i],100)
                        )
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

    @Provides
    @Singleton
    fun provideAzkarCategoryRepository(db: MyDatabase): AzkarCategoryRepository {
        return AzkarCategoryRepositoryImp(db.azkarCategoryDao)
    }

    @Provides
    @Singleton
    fun provideElZekrRepository(db: MyDatabase): ElZekrRepository {
        return ElZekrRepositoryImp(db.elZekrDao)
    }

    @Provides
    @Singleton
    fun provideTasbehRepository(db: MyDatabase): TasbehRepository {
        return TasbehRepositoryImp(db.tasbehDao)
    }


    @Provides
    @Singleton
    fun provideTasbehDataRepository(db: MyDatabase): TasbehDataRepository {
        return TasbehDataRepositoryImp(db.tasbehDataDao)
    }


    //for music
    @Singleton
    @Provides
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context
    ) = MusicServiceConnection(context)

}