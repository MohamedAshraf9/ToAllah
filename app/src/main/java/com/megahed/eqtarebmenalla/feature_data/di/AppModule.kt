package com.megahed.eqtarebmenalla.feature_data.di

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.gson.Gson
import com.megahed.eqtarebmenalla.App
import com.megahed.eqtarebmenalla.R
import com.megahed.eqtarebmenalla.common.Constants
import com.megahed.eqtarebmenalla.db.MIGRATION_3_4
import com.megahed.eqtarebmenalla.db.MIGRATION_4_5
import com.megahed.eqtarebmenalla.db.MIGRATION_5_6
import com.megahed.eqtarebmenalla.db.MIGRATION_6_7
import com.megahed.eqtarebmenalla.db.MIGRATION_7_8
import com.megahed.eqtarebmenalla.db.MyDatabase
import com.megahed.eqtarebmenalla.db.dao.AchievementDao
import com.megahed.eqtarebmenalla.db.dao.CachedRecitersDao
import com.megahed.eqtarebmenalla.db.dao.DailyTargetDao
import com.megahed.eqtarebmenalla.db.dao.DownloadedAudioDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationScheduleDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationSessionDao
import com.megahed.eqtarebmenalla.db.dao.OfflineSettingsDao
import com.megahed.eqtarebmenalla.db.dao.UserStreakDao
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
import com.megahed.eqtarebmenalla.feature_data.data.repository.MemorizationRepository
import com.megahed.eqtarebmenalla.feature_data.data.repository.QuranListenerRepositoryImp
import com.megahed.eqtarebmenalla.feature_data.domain.repository.IslamicRepository
import com.megahed.eqtarebmenalla.feature_data.domain.repository.QuranListenerRepository
import com.megahed.eqtarebmenalla.offline.OfflineAudioManager
import com.megahed.eqtarebmenalla.offline.OfflineUtils.isNetworkAvailable
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Provider
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(@ApplicationContext context: Context): OkHttpClient {
        val cacheSize = 10L * 1024 * 1024
        val cache = Cache(context.cacheDir, cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .addInterceptor { chain ->
                var request = chain.request()
                request = if (isNetworkAvailable(context)) {
                    request.newBuilder()
                        .header("Cache-Control", "public, max-age=" + 300)
                        .build()
                } else {
                    request.newBuilder()
                        .header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 60 * 24 * 7)
                        .build()
                }
                chain.proceed(request)
            }
            .build()
    }
    @Provides
    @Singleton
    fun provideIslamicApi(okHttpClient: OkHttpClient): IslamicApi {
        return Retrofit.Builder()
            .baseUrl(Constants.PRAYER_TIME_BASE_URL)
            .client(okHttpClient)
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
            .fallbackToDestructiveMigration(false)
            .createFromAsset("prepopulated_db.db")
            .build()
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

    @Provides
    @Singleton
    fun provideQuranListenerReaderRepository(db: MyDatabase): QuranListenerReaderRepository {
        return QuranListenerReaderRepositoryImp(db.quranListenerReaderDao)
    }

    @Provides
    @Singleton
    fun provideSoraSongRepository(db: MyDatabase): SoraSongRepository {
        return SoraSongRepositoryImp(db.soraSongDao)
    }


    @Singleton
    @Provides
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context): MusicServiceConnection {
        return MusicServiceConnection(context)
    }


    @Singleton
    @Provides
    fun provideGlideInstance(
        @ApplicationContext context: Context): RequestManager {
        return Glide.with(context).setDefaultRequestOptions(
            RequestOptions()
                .placeholder(R.drawable.ic_image)
                .error(R.drawable.ic_image)
                .diskCacheStrategy(DiskCacheStrategy.DATA)
        )
    }

    @Provides
    fun provideMemorizationScheduleDao(database: MyDatabase): MemorizationScheduleDao =
        database.memorizationScheduleDao()

    @Provides
    fun provideDailyTargetDao(database: MyDatabase): DailyTargetDao =
        database.dailyTargetDao()

    @Provides
    fun provideMemorizationSessionDao(database: MyDatabase): MemorizationSessionDao =
        database.memorizationSessionDao()

    @Provides
    fun provideUserStreakDao(database: MyDatabase): UserStreakDao =
        database.userStreakDao()

    @Provides
    fun provideAchievementDao(database: MyDatabase): AchievementDao =
        database.achievementDao()

    @Provides
    @Singleton
    fun provideMemorizationRepository(
        scheduleDao: MemorizationScheduleDao,
        dailyTargetDao: DailyTargetDao,
        sessionDao: MemorizationSessionDao,
        streakDao: UserStreakDao,
        achievementDao: AchievementDao
    ): MemorizationRepository {
        return MemorizationRepository(
            scheduleDao = scheduleDao,
            dailyTargetDao = dailyTargetDao,
            sessionDao = sessionDao,
            streakDao = streakDao,
            achievementDao = achievementDao
        )
    }
    @Provides
    @Singleton
    fun provideOfflineAudioManager(
        @ApplicationContext context: Context,
        downloadedAudioDao: DownloadedAudioDao,
        offlineSettingsDao: OfflineSettingsDao,
        quranListenerReaderRepository: QuranListenerReaderRepository
    ): OfflineAudioManager {
        return OfflineAudioManager(context, downloadedAudioDao, offlineSettingsDao)
    }



    @Provides
    fun provideDownloadedAudioDao(database: MyDatabase): DownloadedAudioDao {
        return database.downloadedAudioDao()
    }

    @Provides
    fun provideOfflineSettingsDao(database: MyDatabase): OfflineSettingsDao {
        return database.offlineSettingsDao()
    }
    @Provides
    fun provideCachedRecitersDao(database: MyDatabase): CachedRecitersDao {
        return database.cachedRecitersDao()
    }
}