package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlin.coroutines.resume

object GeocodingHelper {

    private const val TAG = "GeocodingHelper"

    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            Log.w(TAG, "Geocoder not present")
            return@withContext null
        }

        val geocoder = Geocoder(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    latitude,
                    longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (addresses.isNotEmpty()) {
                                val address = addresses[0]
                                val country = address.adminArea ?: "Unknown"
                                val city = address.subAdminArea ?: "Unknown"
                                continuation.resume("$city, $country")
                            } else {
                                Log.w(TAG, "No address found for TIRAMISU+")
                                continuation.resume(null)
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            super.onError(errorMessage)
                            Log.e(TAG, "Geocoding error for TIRAMISU+: $errorMessage")
                            continuation.resume(null)
                        }
                    }
                )
                continuation.invokeOnCancellation {
                    Log.d(TAG, "Geocoding for TIRAMISU+ was cancelled")
                }
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val country = address.adminArea ?: "Unknown"
                    val city = address.subAdminArea ?: "Unknown"
                    "$city, $country"
                } else {
                    Log.w(TAG, "No address found for pre-TIRAMISU")
                    null
                }
            } catch (e: IOException) {
                Log.e(TAG, "Geocoder IOException for pre-TIRAMISU: ${e.message}", e)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected geocoding error for pre-TIRAMISU: ${e.message}", e)
                null
            }
        }
    }
}
