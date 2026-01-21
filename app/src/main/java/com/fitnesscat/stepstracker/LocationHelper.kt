package com.fitnesscat.stepstracker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Helper class to get current GPS location
 */
class LocationHelper(private val context: Context) {
    
    companion object {
        private const val TAG = "LocationHelper"
        private const val LOCATION_TIMEOUT_SECONDS = 10L
    }
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    /**
     * Gets the current location (latitude, longitude)
     * Returns null if permission not granted or location not available
     */
    fun getCurrentLocation(callback: (Double?, Double?) -> Unit) {
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            callback(null, null)
            return
        }
        
        // Request location with high accuracy
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d(TAG, "Location obtained: lat=$latitude, lng=$longitude")
                callback(latitude, longitude)
            } else {
                Log.w(TAG, "Location is null")
                callback(null, null)
            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get location: ${exception.message}", exception)
            callback(null, null)
        }
    }
    
    /**
     * Gets the current location synchronously (for use in background workers)
     * Returns Pair<Double?, Double?> (latitude, longitude) or null values if unavailable
     */
    fun getCurrentLocationSync(): Pair<Double?, Double?> {
        var result: Pair<Double?, Double?> = Pair(null, null)
        val latch = CountDownLatch(1)
        
        // Check if location permission is granted
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Location permission not granted")
            return Pair(null, null)
        }
        
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location: Location? ->
            if (location != null) {
                result = Pair(location.latitude, location.longitude)
                Log.d(TAG, "Location obtained: lat=${location.latitude}, lng=${location.longitude}")
            } else {
                Log.w(TAG, "Location is null")
                result = Pair(null, null)
            }
            latch.countDown()
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Failed to get location: ${exception.message}", exception)
            result = Pair(null, null)
            latch.countDown()
        }
        
        // Wait for location with timeout
        try {
            val received = latch.await(LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            if (!received) {
                Log.w(TAG, "Location request timed out")
            }
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while waiting for location", e)
        }
        
        return result
    }
}



