package com.megahed.eqtarebmenalla.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit

object PrayerTimesScheduler {

    fun schedulePrayerTimesWork(context: Context, latitude: Double, longitude: Double) {
        val inputData = workDataOf(
            "latitude" to latitude,
            "longitude" to longitude
        )

        val prayerTimesWorkRequest = PeriodicWorkRequestBuilder<PrayerTimesWorker>(24, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED) // Ensure network is available
                    .build()
            )
            .setInputData(inputData)
            .build()

        // Enqueue the worker to run periodically
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "PrayerTimesWork",
            ExistingPeriodicWorkPolicy.KEEP,   // Keep existing work (so it doesn't duplicate)
            prayerTimesWorkRequest
        )
    }

}