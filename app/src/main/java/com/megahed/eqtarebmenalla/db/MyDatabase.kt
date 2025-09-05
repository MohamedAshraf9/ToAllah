package com.megahed.eqtarebmenalla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.megahed.eqtarebmenalla.db.converter.Converters
import com.megahed.eqtarebmenalla.db.dao.AchievementDao
import com.megahed.eqtarebmenalla.db.dao.AyaDao
import com.megahed.eqtarebmenalla.db.dao.AzkarCategoryDao
import com.megahed.eqtarebmenalla.db.dao.DailyTargetDao
import com.megahed.eqtarebmenalla.db.dao.ElZekrDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationScheduleDao
import com.megahed.eqtarebmenalla.db.dao.MemorizationSessionDao
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
import com.megahed.eqtarebmenalla.db.model.DailyTarget
import com.megahed.eqtarebmenalla.db.model.ElZekr
import com.megahed.eqtarebmenalla.db.model.MemorizationSchedule
import com.megahed.eqtarebmenalla.db.model.MemorizationSession
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
        Achievement::class
    ],
    version = 4,
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
    abstract fun memorizationScheduleDao(): MemorizationScheduleDao
    abstract fun dailyTargetDao(): DailyTargetDao
    abstract fun memorizationSessionDao(): MemorizationSessionDao
    abstract fun userStreakDao(): UserStreakDao
    abstract fun achievementDao(): AchievementDao

}
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {

        db.execSQL("""
            CREATE TABLE memorization_schedules (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                title TEXT NOT NULL,
                description TEXT,
                startDate INTEGER NOT NULL,
                endDate INTEGER NOT NULL,
                isActive INTEGER NOT NULL DEFAULT 1,
                createdAt INTEGER NOT NULL,
                updatedAt INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE daily_targets (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                scheduleId INTEGER NOT NULL,
                targetDate INTEGER NOT NULL,
                surahId INTEGER NOT NULL,
                surahName TEXT NOT NULL,
                startVerse INTEGER NOT NULL,
                endVerse INTEGER NOT NULL,
                estimatedDurationMinutes INTEGER NOT NULL DEFAULT 30,
                isCompleted INTEGER NOT NULL DEFAULT 0,
                completedAt INTEGER,
                FOREIGN KEY(scheduleId) REFERENCES memorization_schedules(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("CREATE INDEX index_daily_targets_scheduleId ON daily_targets(scheduleId)")
        db.execSQL("CREATE INDEX index_daily_targets_targetDate ON daily_targets(targetDate)")

        db.execSQL("""
            CREATE TABLE memorization_sessions (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                dailyTargetId INTEGER NOT NULL,
                startTime INTEGER NOT NULL,
                endTime INTEGER,
                actualDurationMinutes INTEGER NOT NULL DEFAULT 0,
                versesCompleted INTEGER NOT NULL DEFAULT 0,
                sessionType TEXT NOT NULL DEFAULT 'LISTENING',
                notes TEXT,
                FOREIGN KEY(dailyTargetId) REFERENCES daily_targets(id) ON DELETE CASCADE
            )
        """)

        db.execSQL("CREATE INDEX index_memorization_sessions_dailyTargetId ON memorization_sessions(dailyTargetId)")

        db.execSQL("""
            CREATE TABLE user_streaks (
                id INTEGER PRIMARY KEY NOT NULL DEFAULT 1,
                currentStreak INTEGER NOT NULL DEFAULT 0,
                longestStreak INTEGER NOT NULL DEFAULT 0,
                lastCompletionDate INTEGER,
                totalDaysCompleted INTEGER NOT NULL DEFAULT 0,
                totalVersesMemorized INTEGER NOT NULL DEFAULT 0,
                updatedAt INTEGER NOT NULL
            )
        """)

        db.execSQL("""
            CREATE TABLE achievements (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                type TEXT NOT NULL,
                title TEXT NOT NULL,
                description TEXT NOT NULL,
                iconResource TEXT,
                unlockedAt INTEGER NOT NULL,
                value INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}