package com.megahed.eqtarebmenalla.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.megahed.eqtarebmenalla.common.Resource
import com.megahed.eqtarebmenalla.db.model.PrayerTime
import com.megahed.eqtarebmenalla.db.repository.PrayerTimeRepository
import com.megahed.eqtarebmenalla.feature_data.domain.use_cases.PrayerDataUsesCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class PrayerTimesWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val prayerDataUseCase: PrayerDataUsesCase,
    private val prayerTimeRepository: PrayerTimeRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        try {
            val latitude = inputData.getDouble("latitude", 0.0)
            val longitude = inputData.getDouble("longitude", 0.0)

            prayerDataUseCase(latitude, longitude).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        val prayerTimesData = result.data
                        prayerTimesData?.data?.let {
                            val prayerTime= PrayerTime(1,it.date.gregorian.date,it.timings.Asr,it.timings.Dhuhr,
                                it.timings.Fajr,it.timings.Isha,it.timings.Maghrib,it.timings.Sunrise)
                            //isDataAdded=true
                            prayerTimeRepository.insertPrayerTime(prayerTime)

                        }
                    }
                    is Resource.Loading -> {
                        // Handle loading state if needed
                    }
                    is Resource.Error -> {
                        Log.d("PrayerTimesWorker", "doWork: ${result.message}")
                    }
                }
            }

            return Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            return Result.failure()
        }
    }
}