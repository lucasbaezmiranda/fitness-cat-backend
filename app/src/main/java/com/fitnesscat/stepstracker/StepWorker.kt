package com.fitnesscat.stepstracker

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Tasks
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Worker that reads the step counter sensor and GPS location every ~10 minutes,
 * saves step count with coordinates for batch sync.
 */
class StepWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val TAG = "StepWorker"
        private const val SENSOR_READ_TIMEOUT_SECONDS = 5L
        private const val LOCATION_TIMEOUT_SECONDS = 10L
    }

    private val userPreferences = UserPreferences(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

    override fun doWork(): Result {
        Log.d(TAG, "StepWorker started - reading step count and GPS location")

        // Check if sensor is available
        if (stepCounterSensor == null) {
            Log.w(TAG, "Step counter sensor not available")
            return Result.retry()
        }

        // Read current step count from sensor
        val stepCount = readCurrentStepCount()

        if (stepCount == null) {
            Log.w(TAG, "Failed to read step count from sensor - will retry")
            return Result.retry()
        }

        // Get current timestamp
        val timestamp = System.currentTimeMillis()
        val timestampSeconds = timestamp / 1000

        // Get GPS location
        val location = getLastKnownLocation()
        val latitude = location?.latitude
        val longitude = location?.longitude

        Log.d(TAG, "Location: lat=$latitude, lng=$longitude")

        try {
            // Save step record to file
            saveStepCountToFile(stepCount, timestampSeconds)
            Log.d(TAG, "Saved step count to file: steps=$stepCount, timestamp=$timestampSeconds")

            userPreferences.addPendingStepRecord(stepCount, timestampSeconds, latitude, longitude)
            Log.d(TAG, "Added to pending records for batch sync (lat=$latitude, lng=$longitude)")

            // Update widget
            updateWidget()

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in StepWorker: ${e.message}", e)
            return Result.retry()
        }
    }

    /**
     * Gets the last known GPS location using FusedLocationProviderClient.
     * Returns null if no permission or location unavailable.
     */
    private fun getLastKnownLocation(): Location? {
        val context = applicationContext

        // Check location permission
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (!hasFineLocation && !hasCoarseLocation) {
            Log.w(TAG, "No location permission - skipping GPS")
            return null
        }

        return try {
            val fusedClient = LocationServices.getFusedLocationProviderClient(context)
            val locationTask = fusedClient.lastLocation
            val location = Tasks.await(locationTask, LOCATION_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (location != null) {
                Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
            } else {
                Log.w(TAG, "Last location is null (GPS may not have a fix yet)")
            }

            location
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException getting location: ${e.message}")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location: ${e.message}")
            null
        }
    }

    /**
     * Saves step count to a .txt file in app's external files directory
     * Format: timestamp,steps
     */
    private fun saveStepCountToFile(stepCount: Int, timestamp: Long) {
        val context = applicationContext
        val stepsDir = context.getExternalFilesDir("steps") ?: return

        if (!stepsDir.exists()) {
            stepsDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val filename = "steps_$dateStr.txt"
        val file = File(stepsDir, filename)

        FileWriter(file, true).use { writer ->
            writer.append("$timestamp,$stepCount\n")
        }

        Log.d(TAG, "Saved to file: ${file.absolutePath}")
    }

    private fun updateWidget() {
        try {
            val appWidgetManager = AppWidgetManager.getInstance(applicationContext)
            val widgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(applicationContext, StepsWidgetProvider::class.java)
            )
            if (widgetIds.isNotEmpty()) {
                StepsWidgetProvider().onUpdate(applicationContext, appWidgetManager, widgetIds)
                Log.d(TAG, "Widget updated")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update widget: ${e.message}")
        }
    }

    /**
     * Reads the current step count from the sensor
     * Uses a CountDownLatch to wait for sensor event
     */
    private fun readCurrentStepCount(): Int? {
        if (stepCounterSensor == null) {
            return null
        }

        var result: Int? = null
        val latch = CountDownLatch(1)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
                    val currentSensorValue = event.values[0]
                    val lastSensorValue = userPreferences.getLastSensorValue()
                    val totalStepCount = userPreferences.getTotalStepCount()

                    if (lastSensorValue == 0f) {
                        result = currentSensorValue.toInt()
                        userPreferences.setLastSensorValue(currentSensorValue)
                        if (result!! > 0) {
                            userPreferences.setTotalStepCount(result!!)
                        }
                    } else if (currentSensorValue < lastSensorValue) {
                        val stepsSinceReboot = currentSensorValue.toInt()
                        result = totalStepCount + stepsSinceReboot
                        userPreferences.setLastSensorValue(currentSensorValue)
                        if (stepsSinceReboot > 0) {
                            userPreferences.setTotalStepCount(result!!)
                        }
                    } else {
                        val stepsSinceLastReading = (currentSensorValue - lastSensorValue).toInt()
                        result = totalStepCount + stepsSinceLastReading
                        userPreferences.setLastSensorValue(currentSensorValue)
                        if (stepsSinceLastReading > 0) {
                            userPreferences.setTotalStepCount(result!!)
                        }
                    }

                    sensorManager.unregisterListener(this)
                    latch.countDown()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        try {
            val registered = sensorManager.registerListener(
                listener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )

            if (!registered) {
                Log.e(TAG, "Failed to register sensor listener")
                return null
            }

            val received = latch.await(SENSOR_READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)

            if (!received) {
                Log.w(TAG, "Sensor read timeout - unregistering listener")
                sensorManager.unregisterListener(listener)
                return null
            }

            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sensor: ${e.message}", e)
            try {
                sensorManager.unregisterListener(listener)
            } catch (e2: Exception) {
                // Ignore
            }
            return null
        }
    }
}
