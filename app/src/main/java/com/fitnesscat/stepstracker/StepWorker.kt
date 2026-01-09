package com.fitnesscat.stepstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Worker that reads the step counter sensor every 1 hour
 * and saves the current step count to a .txt file
 */
class StepWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    
    companion object {
        private const val TAG = "StepWorker"
        private const val SENSOR_READ_TIMEOUT_SECONDS = 5L
    }
    
    private val userPreferences = UserPreferences(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    override fun doWork(): Result {
        Log.d(TAG, "StepWorker started - reading current step count")
        
        // Check if sensor is available
        if (stepCounterSensor == null) {
            Log.w(TAG, "Step counter sensor not available")
            return Result.retry() // Retry later
        }
        
        // Read current step count from sensor
        val stepCount = readCurrentStepCount()
        
        if (stepCount == null) {
            Log.w(TAG, "Failed to read step count from sensor - will retry")
            // Use exponential backoff for retries
            return Result.retry()
        }
        
        // Get current timestamp
        val timestamp = System.currentTimeMillis()
        val timestampSeconds = timestamp / 1000
        
        // Save to .txt file
        try {
            saveStepCountToFile(stepCount, timestampSeconds)
            Log.d(TAG, "✓ Saved step count to file: steps=$stepCount, timestamp=$timestampSeconds")
            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error saving step count to file: ${e.message}", e)
            return Result.retry() // Retry if save failed
        }
    }
    
    /**
     * Saves step count to a .txt file in app's external files directory
     * Format: timestamp,steps
     */
    private fun saveStepCountToFile(stepCount: Int, timestamp: Long) {
        val context = applicationContext
        val stepsDir = context.getExternalFilesDir("steps") ?: return
        
        // Ensure directory exists
        if (!stepsDir.exists()) {
            stepsDir.mkdirs()
        }
        
        // Create filename with date (one file per day)
        val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
        val dateStr = dateFormat.format(Date())
        val filename = "steps_$dateStr.txt"
        val file = File(stepsDir, filename)
        
        // Append line to file: timestamp,steps
        FileWriter(file, true).use { writer ->
            writer.append("$timestamp,$stepCount\n")
        }
        
        Log.d(TAG, "Saved to file: ${file.absolutePath}")
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
                    
                    // Calculate current total steps
                    if (lastSensorValue == 0f) {
                        // First reading - use sensor value as baseline
                        result = currentSensorValue.toInt()
                        userPreferences.setLastSensorValue(currentSensorValue)
                        if (result!! > 0) {
                            userPreferences.setTotalStepCount(result!!)
                        }
                    } else if (currentSensorValue < lastSensorValue) {
                        // Sensor reset (reboot) - add steps since reboot
                        val stepsSinceReboot = currentSensorValue.toInt()
                        result = totalStepCount + stepsSinceReboot
                        userPreferences.setLastSensorValue(currentSensorValue)
                        if (stepsSinceReboot > 0) {
                            userPreferences.setTotalStepCount(result!!)
                        }
                    } else {
                        // Normal case - calculate difference
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
            
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not needed
            }
        }
        
        try {
            // Register listener
            val registered = sensorManager.registerListener(
                listener,
                stepCounterSensor,
                SensorManager.SENSOR_DELAY_NORMAL
            )
            
            if (!registered) {
                Log.e(TAG, "Failed to register sensor listener")
                return null
            }
            
            // Wait for sensor event (with timeout)
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




