package com.fitnesscat.stepstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Step counter using Android's SensorManager
 * Uses TYPE_STEP_COUNTER which provides cumulative steps since last reboot
 * 
 * This implementation tracks steps persistently across app restarts by:
 * 1. Storing total accumulated steps
 * 2. Tracking the last sensor reading
 * 3. Calculating the difference when sensor updates
 */
class StepCounter(
    private val context: Context,
    private val userPreferences: UserPreferences
) : SensorEventListener {

    private val sensorManager: SensorManager = 
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val stepCounterSensor: Sensor? = 
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    
    private var lastSensorValue: Float = userPreferences.getLastSensorValue()
    private var isRegistered = false

    // Total steps accumulated (persists across app restarts)
    var stepCount: Int = userPreferences.getTotalStepCount()
        private set

    var onStepCountChanged: ((Int) -> Unit)? = null

    init {
        // Load total step count from saved state
        stepCount = userPreferences.getTotalStepCount()
    }

    fun startListening() {
        try {
            stepCounterSensor?.let { sensor ->
                if (!isRegistered) {
                    // Register listener - this will trigger onSensorChanged with current cumulative value
                    // The sensor provides cumulative steps since device boot
                    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
                    isRegistered = true
                }
            } ?: run {
                // Sensor not available
                android.util.Log.w("StepCounter", "Step counter sensor not available")
                onStepCountChanged?.invoke(stepCount)
            }
        } catch (e: SecurityException) {
            android.util.Log.e("StepCounter", "Permission denied for step counter: ${e.message}")
            onStepCountChanged?.invoke(stepCount)
        } catch (e: Exception) {
            android.util.Log.e("StepCounter", "Error starting listener: ${e.message}", e)
            onStepCountChanged?.invoke(stepCount)
        }
    }
    
    /**
     * Force update step count by reading current sensor value
     * This is called when app reopens to catch up on steps taken while app was closed
     */
    fun updateStepsFromSensor() {
        // The sensor will automatically trigger onSensorChanged when registered
        // This method is just a placeholder - the actual update happens in onSensorChanged
        // when the sensor provides the current cumulative value
    }

    fun stopListening() {
        if (isRegistered) {
            sensorManager.unregisterListener(this)
            isRegistered = false
            // Save current state
            userPreferences.setTotalStepCount(stepCount)
            userPreferences.setLastSensorValue(lastSensorValue)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSensorValue = event.values[0]
            
            // If this is the first reading ever (lastSensorValue is 0)
            if (lastSensorValue == 0f) {
                // First time using the app - store current sensor value as baseline
                // Don't add to step count yet (sensor shows steps since boot, not our total)
                lastSensorValue = currentSensorValue
                userPreferences.setLastSensorValue(lastSensorValue)
                // Display current total (which should be 0 for first time)
                onStepCountChanged?.invoke(stepCount)
                return
            }
            
            // Calculate steps taken since last reading
            val stepsSinceLastReading = (currentSensorValue - lastSensorValue).toInt()
            
            // Check if sensor was reset (device rebooted - sensor value would be lower)
            if (currentSensorValue < lastSensorValue) {
                // Device was rebooted - sensor reset to 0 or low value
                // The steps taken since boot are in currentSensorValue
                // We should add those to our total
                val stepsSinceReboot = currentSensorValue.toInt()
                if (stepsSinceReboot > 0) {
                    stepCount += stepsSinceReboot
                    userPreferences.setTotalStepCount(stepCount)
                }
                lastSensorValue = currentSensorValue
                userPreferences.setLastSensorValue(lastSensorValue)
                onStepCountChanged?.invoke(stepCount)
                return
            }
            
            // Normal case: sensor value increased (new steps taken)
            if (stepsSinceLastReading > 0) {
                // Add new steps to total
                stepCount += stepsSinceLastReading
                
                // Save updated values
                lastSensorValue = currentSensorValue
                userPreferences.setTotalStepCount(stepCount)
                userPreferences.setLastSensorValue(lastSensorValue)
                
                // Notify UI
                onStepCountChanged?.invoke(stepCount)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not typically needed for step counter
    }
}

