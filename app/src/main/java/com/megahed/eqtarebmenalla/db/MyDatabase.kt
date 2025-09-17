package com.megahed.eqtarebmenalla.db

import android.annotation.SuppressLint
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.megahed.eqtarebmenalla.db.converter.Converters
import com.megahed.eqtarebmenalla.db.dao.AchievementDao
import com.megahed.eqtarebmenalla.db.dao.AyaDao
import com.megahed.eqtarebmenalla.db.dao.AzkarCategoryDao
import com.megahed.eqtarebmenalla.db.dao.CachedRecitersDao
import com.megahed.eqtarebmenalla.db.dao.DailyTargetDao
import com.megahed.eqtarebmenalla.db.dao.DownloadedAudioDao
import com.megahed.eqtarebmenalla.db.dao.ElZekrDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationScheduleDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationSessionDao
import com.megahed.eqtarebmenalla.db.dao.OfflineSettingsDao
import com.megahed.eqtarebmenalla.db.dao.PrayerTimeDao
import com.megahed.eqtarebmenalla.db.dao.QuranListenerReaderDao
import com.megahed.eqtarebmenalla.db.dao.SoraDao
import com.megahed.eqtarebmenalla.db.dao.SoraSongDao
import com.megahed.eqtarebmenalla.db.dao.TasbehDao
import com.megahed.eqtarebmenalla.db.dao.TasbehDataDao
import com.megahed.eqtarebmenalla.db.dao.UserStreakDao
import com.megahed.eqtarebmenalla.db.model.Achievement
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.AzkarCategory
import com.megahed.eqtarebmenalla.db.model.CachedReciter
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.DownloadedAudio
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.db.model.MemorizationSession
import com.megahed.eqtarebmenalla.db.model.OfflineSettings
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.model.QuranListenerReader
import com.megahed.eqtarebmenalla.db.model.Sora
import com.megahed.eqtarebmenalla.db.model.SoraSong
import com.megahed.eqtarebmenalla.db.model.Tasbeh
import com.megahed.eqtarebmenalla.db.model.TasbehData
import com.megahed.eqtarebmenalla.db.model.UserStreak

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
        SoraSong::class,
        MemorizationSchedule::class,
        DailyTarget::class,
        MemorizationSession::class,
        UserStreak::class,
        Achievement::class,
        DownloadedAudio::class,
        OfflineSettings::class,
        CachedReciter::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MyDatabase : RoomDatabase() {

    companion object {
        const val DATABASE_NAME = "IslamicLocalDB"
    }

    abstract val prayerTime: PrayerTimeDao
    abstract val soraDao: SoraDao
    abstract val ayaDao: AyaDao
    abstract val azkarCategoryDao: AzkarCategoryDao
    abstract val elZekrDao: ElZekrDao

    abstract val tasbehDao: TasbehDao
    abstract val tasbehDataDao: TasbehDataDao
    abstract val quranListenerReaderDao: QuranListenerReaderDao
    abstract val soraSongDao: SoraSongDao
    abstract fun memorizationScheduleDao(): MemorizationScheduleDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun memorizationSessionDao(): MemorizationSessionDao
    abstract fun userStreakDao(): UserStreakDao
    abstract fun achievementDao(): AchievementDao
    abstract fun downloadedAudioDao(): DownloadedAudioDao
    abstract fun offlineSettingsDao(): OfflineSettingsDao
    abstract fun cachedRecitersDao(): CachedRecitersDao
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {

    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `downloaded_audio` (
                    `id` TEXT NOT NULL,
                    `readerId` TEXT NOT NULL,
                    `surahId` INTEGER NOT NULL,
                    `verseId` INTEGER,
                    `surahName` TEXT NOT NULL,
                    `readerName` TEXT NOT NULL,
                    `localFilePath` TEXT NOT NULL,
                    `originalUrl` TEXT NOT NULL,
                    `downloadDate` INTEGER NOT NULL,
                    `fileSize` INTEGER NOT NULL,
                    `isComplete` INTEGER NOT NULL DEFAULT 0,
                    `downloadType` TEXT NOT NULL DEFAULT 'FULL_SURAH',
                    PRIMARY KEY(`id`)
                )
            """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `offline_settings` (
                    `id` INTEGER NOT NULL,
                    `isOfflineMemorizationEnabled` INTEGER NOT NULL DEFAULT 0,
                    `selectedOfflineReaderId` TEXT,
                    `selectedOfflineReaderName` TEXT,
                    `totalDownloadedSize` INTEGER NOT NULL DEFAULT 0,
                    PRIMARY KEY(`id`)
                )
            """
            )

            db.execSQL(
                """
                INSERT OR REPLACE INTO `offline_settings` 
                (`id`, `isOfflineMemorizationEnabled`, `selectedOfflineReaderId`, `selectedOfflineReaderName`, `totalDownloadedSize`) 
                VALUES (1, 0, NULL, NULL, 0)
            """
            )
        } catch (e: Exception) {
            throw RuntimeException("Migration 5->6 failed", e)
        }
    }
}
val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS cached_reciters (
                id INTEGER NOT NULL PRIMARY KEY,
                name TEXT NOT NULL,
                audio_url_bit_rate_32_ TEXT NOT NULL,
                audio_url_bit_rate_64 TEXT NOT NULL,
                audio_url_bit_rate_128 TEXT NOT NULL,
                cachedAt INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }
}
val MIGRATION_7_8 = object : Migration(7, 8) {
    @SuppressLint("Range")
    override fun migrate(database: SupportSQLiteDatabase) {
        // Always create the table with IF NOT EXISTS to handle missing table scenarios
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `daily_targets` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `scheduleId` INTEGER NOT NULL,
                `targetDate` INTEGER NOT NULL,
                `surahId` INTEGER NOT NULL,
                `surahName` TEXT NOT NULL,
                `startVerse` INTEGER NOT NULL,
                `endVerse` INTEGER NOT NULL,
                `estimatedDurationMinutes` INTEGER NOT NULL DEFAULT 30,
                `isCompleted` INTEGER NOT NULL DEFAULT 0,
                `completedAt` INTEGER,
                `completedVerses` INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(`scheduleId`) REFERENCES `memorization_schedules`(`id`) ON DELETE CASCADE
            )
        """.trimIndent())

        // Create indices if they don't exist
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_targets_scheduleId` ON `daily_targets` (`scheduleId`)")
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_daily_targets_targetDate` ON `daily_targets` (`targetDate`)")

        // For existing tables, we need to check if completedVerses column exists and add it if not
        val cursor = database.query("PRAGMA table_info(daily_targets)")
        var hasCompletedVerses = false
        while (cursor.moveToNext()) {
            val columnName = cursor.getString(cursor.getColumnIndex("name"))
            if (columnName == "completedVerses") {
                hasCompletedVerses = true
                break
            }
        }
        cursor.close()

        // If the table existed but doesn't have completedVerses column, add it
        if (!hasCompletedVerses) {
            try {
                database.execSQL("ALTER TABLE daily_targets ADD COLUMN completedVerses INTEGER NOT NULL DEFAULT 0")
            } catch (e: Exception) {
                // Column might already exist or table structure is different
                android.util.Log.w("Migration", "Could not add completedVerses column: ${e.message}")
            }
        }
    }
}