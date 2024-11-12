package com.megahed.eqtarebmenalla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.megahed.eqtarebmenalla.db.dao.AyaDao
import com.megahed.eqtarebmenalla.db.dao.AzkarCategoryDao
import com.megahed.eqtarebmenalla.db.dao.ElZekrDao
import com.megahed.eqtarebmenalla.db.dao.PrayerTimeDao
import com.megahed.eqtarebmenalla.db.dao.QuranListenerReaderDao
import com.megahed.eqtarebmenalla.db.dao.SoraDao
import com.megahed.eqtarebmenalla.db.dao.SoraSongDao
import com.megahed.eqtarebmenalla.db.dao.TasbehDao
import com.megahed.eqtarebmenalla.db.dao.TasbehDataDao
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehData

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
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the 'url' column to the 'Aya' table
                db.execSQL("ALTER TABLE Aya ADD COLUMN url TEXT")
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