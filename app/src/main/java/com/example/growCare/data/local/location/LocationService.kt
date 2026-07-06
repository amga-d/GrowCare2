package com.example.growCare.data.local.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Service to get user's current location
 * Uses Google Play Services FusedLocationProviderClient
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "LocationService"
    }

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get current location
     * @return Location or null if unavailable
     */
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        Log.d(TAG, "getCurrentLocation: Starting location fetch")

        if (!hasLocationPermission()) {
            Log.w(TAG, "getCurrentLocation: No location permission")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        if (!isLocationEnabled()) {
            Log.w(TAG, "getCurrentLocation: Location services disabled")
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }

        try {
            val cancellationTokenSource = CancellationTokenSource()

            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                cancellationTokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "getCurrentLocation: Success - Lat: ${location.latitude}, Lon: ${location.longitude}")
                    continuation.resume(location)
                } else {
                    Log.w(TAG, "getCurrentLocation: Got null location, trying last known")
                    // If current location is null, try last known location
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                            if (lastLocation != null) {
                                Log.d(TAG, "getCurrentLocation: Using last known location - Lat: ${lastLocation.latitude}, Lon: ${lastLocation.longitude}")
                            } else {
                                Log.w(TAG, "getCurrentLocation: Last known location is also null")
                            }
                            continuation.resume(lastLocation)
                        }.addOnFailureListener { e ->
                            Log.e(TAG, "getCurrentLocation: Failed to get last known location", e)
                            continuation.resume(null)
                        }
                    } catch (e: SecurityException) {
                        Log.e(TAG, "getCurrentLocation: SecurityException when getting last known location", e)
                        continuation.resume(null)
                    }
                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "getCurrentLocation: Failed, trying last known location", exception)
                // If current location fails, try last known location
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                        if (lastLocation != null) {
                            Log.d(TAG, "getCurrentLocation: Using last known location after error - Lat: ${lastLocation.latitude}, Lon: ${lastLocation.longitude}")
                        } else {
                            Log.w(TAG, "getCurrentLocation: Last known location is null after error")
                        }
                        continuation.resume(lastLocation)
                    }.addOnFailureListener { e ->
                        Log.e(TAG, "getCurrentLocation: Failed to get last known location after error", e)
                        continuation.resume(null)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "getCurrentLocation: SecurityException after error", e)
                    continuation.resume(null)
                }
            }

            continuation.invokeOnCancellation {
                Log.d(TAG, "getCurrentLocation: Cancelled")
                cancellationTokenSource.cancel()
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "getCurrentLocation: SecurityException", e)
            continuation.resume(null)
        }
    }

    /**
     * Get last known location (faster but may be stale)
     */
    suspend fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) {
            return null
        }

        return try {
            fusedLocationClient.lastLocation.await()
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}

