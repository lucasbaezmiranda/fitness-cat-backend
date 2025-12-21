package com.fitnesscat.stepstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Worker that runs periodically (every 30 minutes) to read step counter sensor
 * and save the data locally for batch sync later
 * 
 * This runs in background without notification - most efficient solution
 */
class StepWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params), SensorEventListener {

    private val userPreferences = UserPreferences(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private var sensorValueRead = false
    private var currentSensorValue: Float = 0f

    override suspend fun doWork(): Result = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "StepWorker: Starting periodic step reading")
            
            // Check permission
            val hasPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    applicationContext,
                    android.Manifest.permission.ACTIVITY_RECOGNITION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
            
            if (!hasPermission) {
                Log.w(TAG, "StepWorker: Permission not granted, skipping")
                return@withContext Result.retry()
            }
            
            if (stepCounterSensor == null) {
                Log.w(TAG, "StepWorker: Sensor not available, skipping")
                return@withContext Result.failure()
            }
            
            // Read sensor value (this will trigger onSensorChanged)
            // Note: Sensor listeners must be registered on Main thread
            sensorValueRead = false
            sensorManager.registerListener(this@StepWorker, stepCounterSensor, SensorManager.SENSOR_DELAY_NORMAL)
            
            // Wait up to 3 seconds for sensor to fire
            var waitCount = 0
            while (!sensorValueRead && waitCount < 30) {
                kotlinx.coroutines.delay(100)
                waitCount++
            }
            
            sensorManager.unregisterListener(this@StepWorker)
            
            if (!sensorValueRead) {
                Log.w(TAG, "StepWorker: Sensor didn't fire in time")
                return@withContext Result.retry()
            }
            
            // Save to local JSON for batch sync (switch to IO thread for file operations)
            withContext(Dispatchers.IO) {
                saveStepReading(currentSensorValue)
            }
            
            Log.d(TAG, "StepWorker: Successfully saved step reading: $currentSensorValue")
            Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "StepWorker: Error reading steps: ${e.message}", e)
            Result.retry()
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            currentSensorValue = event.values[0]
            sensorValueRead = true
            Log.d(TAG, "StepWorker: Sensor value read: $currentSensorValue")
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
    
    /**
     * Saves step reading to local JSON for batch sync
     */
    private fun saveStepReading(sensorValue: Float) {
        try {
            // Load existing pending records
            val pendingJson = userPreferences.getPendingStepRecords()
            val records = if (pendingJson.isNotEmpty()) {
                JSONArray(pendingJson)
            } else {
                JSONArray()
            }
            
            // Get current step count
            val totalSteps = userPreferences.getTotalStepCount()
            val lastSensorValue = userPreferences.getLastSensorValue()
            
            // Calculate new steps if we have previous value
            var newSteps = 0
            if (lastSensorValue > 0f && sensorValue > lastSensorValue) {
                newSteps = (sensorValue - lastSensorValue).toInt()
            }
            
            // Create new record
            val record = JSONObject().apply {
                put("timestamp", System.currentTimeMillis() / 1000) // Unix timestamp in seconds
                put("sensor_value", sensorValue.toDouble())
                put("steps_at_time", totalSteps + newSteps)
                put("new_steps", newSteps)
            }
            
            records.put(record)
            
            // Save back to preferences
            userPreferences.savePendingStepRecords(records.toString())
            
            // Update last sensor value
            userPreferences.setLastSensorValue(sensorValue)
            
            // Update total steps if we have new steps
            if (newSteps > 0) {
                userPreferences.setTotalStepCount(totalSteps + newSteps)
            }
            
            Log.d(TAG, "StepWorker: Saved record - sensor: $sensorValue, new steps: $newSteps, total: ${totalSteps + newSteps}")
            
        } catch (e: Exception) {
            Log.e(TAG, "StepWorker: Error saving step reading: ${e.message}", e)
        }
    }
    
    companion object {
        private const val TAG = "StepWorker"
    }
}

