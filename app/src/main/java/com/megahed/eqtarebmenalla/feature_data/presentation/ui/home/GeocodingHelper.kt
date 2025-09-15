package com.megahed.eqtarebmenalla.feature_data.presentation.ui.home

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import kotlin.coroutines.resume

object GeocodingHelper {


    suspend fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double
    ): String? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) {
            return@withContext null
        }

        val geocoder = Geocoder(context, Locale.getDefault())

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
                                val locationString = formatLocationString(address)
                                continuation.resume(locationString)
                            } else {
                                continuation.resume(getDefaultLocation())
                            }
                        }

                        override fun onError(errorMessage: String?) {
                            super.onError(errorMessage)
                            continuation.resume(getDefaultLocation())
                        }
                    }
                )
                continuation.invokeOnCancellation {
                }
            }
        } else {
            @Suppress("DEPRECATION")
            try {
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (addresses != null && addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val locationString = formatLocationString(address)
                    locationString
                } else {
                    getDefaultLocation()
                }
            } catch (_: IOException) {
                getDefaultLocation()
            } catch (_: Exception) {
                getDefaultLocation()
            }
        }
    }

    private fun formatLocationString(address: Address): String {
        val city = address.locality
            ?: address.subAdminArea
            ?: "Unknown City"

        val country = address.adminArea
            ?: "Unknown Country"

        return "$city, $country"
    }

    private fun getDefaultLocation(): String {
        return when (Locale.getDefault().language) {
            "ar" -> "موقع غير معروف"
            else -> "Unknown Location"
        }
    }
}