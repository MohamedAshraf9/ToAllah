package com.megahed.eqtarebmenalla.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.megahed.eqtarebmenalla.db.dao.AyaDao
import com.megahed.eqtarebmenalla.db.dao.PrayerTimeDao
import com.megahed.eqtarebmenalla.db.dao.SoraDao
import com.megahed.eqtarebmenalla.db.model.Aya
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.model.Sora

@Database(
    entities = [
        PrayerTime::class,
        Sora::class,
        Aya::class,
    ],
    version = 1,
    exportSchema = false
)
abstract class MyDatabase : RoomDatabase() {

    companion object{
        const val DATABASE_NAME="IslamicLocalDB"
    }

    abstract val prayerTime:PrayerTimeDao
    abstract val soraDao:SoraDao
    abstract val ayaDao:AyaDao

}