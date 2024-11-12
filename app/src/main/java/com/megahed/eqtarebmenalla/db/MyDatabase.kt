package com.megahed.eqtarebmenalla.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyDatabase : RoomDatabase() {

    companion object{
        const val DATABASE_NAME="IslamicLocalDB"

        val MIGRATION_1_3 = object : Migration(1, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the 'url' column to the 'Aya' table
                database.execSQL("ALTER TABLE Aya ADD COLUMN url TEXT")
            }
        }
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