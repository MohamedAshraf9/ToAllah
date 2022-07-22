package com.megahed.eqtarebmenalla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.megahed.eqtarebmenalla.db.dao.*
import com.megahed.eqtarebmenalla.db.model.*

@Database(
    entities = [
        PrayerTime::class,
        Sora::class,
        Aya::class,
        AzkarCategory::class,
        ElZekr::class,
        Tasbeh::class,
        TasbehData::class,
        QuranListenerReader::class,
        SoraSong::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyDatabase : RoomDatabase() {

    companion object{
        const val DATABASE_NAME="IslamicLocalDB"
    }

    abstract val prayerTime:PrayerTimeDao
    abstract val soraDao:SoraDao
    abstract val ayaDao:AyaDao
    abstract val azkarCategoryDao:AzkarCategoryDao
    abstract val elZekrDao:ElZekrDao

    abstract val tasbehDao:TasbehDao
    abstract val tasbehDataDao:TasbehDataDao
    abstract val quranListenerReaderDao:QuranListenerReaderDao
    abstract val soraSongDao:SoraSongDao


}