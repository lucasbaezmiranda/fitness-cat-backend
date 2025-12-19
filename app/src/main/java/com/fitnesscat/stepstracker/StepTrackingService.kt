package com.fitnesscat.stepstracker

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat

/**
 * Foreground service that continuously tracks steps in the background
 * This allows step tracking even when the app is closed
 */
class StepTrackingService : Service(), SensorEventListener {
    
    private lateinit var sensorManager: SensorManager
    private var stepCounterSensor: Sensor? = null
    private lateinit var userPreferences: UserPreferences
    
    private var lastSensorValue: Float = 0f
    private var totalStepCount: Int = 0
    private var sensorReadTimeout: Handler? = null
    private val sensorReadTimeoutDelay = 3000L // Wait 3 seconds for sensor to fire
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            userPreferences = UserPreferences(this)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            
            // Load saved state
            totalStepCount = userPreferences.getTotalStepCount()
            lastSensorValue = userPreferences.getLastSensorValue()
            
            // Don't use foreground service - no notification needed
            // We'll just read the sensor value once and stop
            android.util.Log.d("StepTrackingService", "Service started - will read sensor and stop")
            
            // Check permission before accessing sensor
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not needed on older versions
            }
            
            // Read sensor value once and update steps
            if (hasPermission && stepCounterSensor != null) {
                android.util.Log.d("StepTrackingService", "Reading sensor - permission: granted, sensor: available")
                android.util.Log.d("StepTrackingService", "Current state - totalSteps: $totalStepCount, lastSensorValue: $lastSensorValue")
                
                // Register listener to get current sensor value
                startStepTracking()
                
                // Set timeout to stop service if sensor doesn't fire within 3 seconds
                sensorReadTimeout = Handler(Looper.getMainLooper())
                sensorReadTimeout?.postDelayed({
                    android.util.Log.w("StepTrackingService", "Sensor read timeout - stopping service")
                    stopSelf()
                }, sensorReadTimeoutDelay)
                
                // Stop service after reading (sensor will fire once with current value)
                // We'll stop in onSensorChanged after processing
            } else {
                if (!hasPermission) {
                    android.util.Log.w("StepTrackingService", "Activity Recognition permission not granted")
                }
                if (stepCounterSensor == null) {
                    android.util.Log.w("StepTrackingService", "Step counter sensor not available")
                    val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
                    android.util.Log.d("StepTrackingService", "Available sensors: ${sensorList.map { "${it.name} (type=${it.type})" }}")
                }
                // Stop service if sensor/permission not available
                stopSelf()
            }
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "Error in onCreate: ${e.message}", e)
            // Don't stop service on error - let it try to continue
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Don't restart - we only need to read sensor once
        return START_NOT_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Step Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks your steps in the background"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Step Tracker Running")
            .setContentText("Tracking your steps: $totalStepCount")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    private fun updateNotification() {
        val notification = createNotification()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    private fun startStepTracking() {
        try {
            stepCounterSensor?.let { sensor ->
                // Use SENSOR_DELAY_NORMAL for better responsiveness
                // TYPE_STEP_COUNTER only fires when steps change, so delay doesn't matter much
                android.util.Log.d("StepTrackingService", "Attempting to register sensor listener...")
                android.util.Log.d("StepTrackingService", "Sensor details:")
                android.util.Log.d("StepTrackingService", "  - Name: ${sensor.name}")
                android.util.Log.d("StepTrackingService", "  - Vendor: ${sensor.vendor}")
                android.util.Log.d("StepTrackingService", "  - Type: ${sensor.type} (TYPE_STEP_COUNTER=${Sensor.TYPE_STEP_COUNTER})")
                android.util.Log.d("StepTrackingService", "  - Max Range: ${sensor.maximumRange}")
                android.util.Log.d("StepTrackingService", "  - Resolution: ${sensor.resolution}")
                android.util.Log.d("StepTrackingService", "  - Power: ${sensor.power}mA")
                
                val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                if (registered) {
                    android.util.Log.d("StepTrackingService", "✓ Successfully registered step counter sensor listener")
                    android.util.Log.d("StepTrackingService", "Waiting for sensor events... (sensor will fire when steps change)")
                    android.util.Log.d("StepTrackingService", "Current state: totalSteps=$totalStepCount, lastSensorValue=$lastSensorValue")
                } else {
                    android.util.Log.e("StepTrackingService", "✗ Failed to register sensor listener - registerListener returned false")
                }
            } ?: run {
                android.util.Log.e("StepTrackingService", "✗ Step counter sensor is null - device may not have step counter hardware")
            }
        } catch (e: SecurityException) {
            android.util.Log.e("StepTrackingService", "✗ SecurityException registering sensor: ${e.message}", e)
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "✗ Error starting step tracking: ${e.message}", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSensorValue = event.values[0]
            android.util.Log.d("StepTrackingService", "=== SENSOR EVENT ===")
            android.util.Log.d("StepTrackingService", "Current sensor value: $currentSensorValue")
            android.util.Log.d("StepTrackingService", "Last sensor value: $lastSensorValue")
            android.util.Log.d("StepTrackingService", "Total step count: $totalStepCount")
            
            // Cancel timeout since sensor fired
            sensorReadTimeout?.removeCallbacksAndMessages(null)
            
            // First reading - just store baseline
            if (lastSensorValue == 0f) {
                android.util.Log.d("StepTrackingService", "First sensor reading - storing baseline: $currentSensorValue")
                android.util.Log.d("StepTrackingService", "Note: Steps already taken since boot ($currentSensorValue) will not be counted")
                android.util.Log.d("StepTrackingService", "Only new steps after this point will be tracked")
                lastSensorValue = currentSensorValue
                userPreferences.setLastSensorValue(lastSensorValue)
                // Stop service after reading
                stopSelfSafely()
                return
            }
            
            // Check for reboot (sensor reset)
            if (currentSensorValue < lastSensorValue) {
                android.util.Log.d("StepTrackingService", "Sensor reset detected (reboot) - current: $currentSensorValue, last: $lastSensorValue")
                val stepsSinceReboot = currentSensorValue.toInt()
                if (stepsSinceReboot > 0) {
                    totalStepCount += stepsSinceReboot
                    userPreferences.setTotalStepCount(totalStepCount)
                    android.util.Log.d("StepTrackingService", "Added $stepsSinceReboot steps since reboot. New total: $totalStepCount")
                }
                lastSensorValue = currentSensorValue
                userPreferences.setLastSensorValue(lastSensorValue)
                // Stop service after updating
                stopSelfSafely()
                return
            }
            
            // Calculate new steps
            val stepsSinceLastReading = (currentSensorValue - lastSensorValue).toInt()
            android.util.Log.d("StepTrackingService", "Steps since last reading: $stepsSinceLastReading")
            
            if (stepsSinceLastReading > 0) {
                totalStepCount += stepsSinceLastReading
                lastSensorValue = currentSensorValue
                
                // Save to preferences
                userPreferences.setTotalStepCount(totalStepCount)
                userPreferences.setLastSensorValue(lastSensorValue)
                
                android.util.Log.d("StepTrackingService", "✓ Added $stepsSinceLastReading new steps!")
                android.util.Log.d("StepTrackingService", "✓ New total: $totalStepCount steps")
                
                // Stop service after updating - we only needed to read once
                stopSelfSafely()
            } else if (stepsSinceLastReading == 0) {
                android.util.Log.d("StepTrackingService", "No new steps detected (sensor value unchanged)")
            } else {
                android.util.Log.w("StepTrackingService", "WARNING: Negative step difference! This shouldn't happen unless sensor reset.")
                android.util.Log.w("StepTrackingService", "currentValue=$currentSensorValue, lastValue=$lastSensorValue")
            }
        } else {
            android.util.Log.w("StepTrackingService", "Received sensor event for wrong sensor type: ${event?.sensor?.type}")
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
    
    /**
     * Safely stops the service after unregistering sensor listener
     */
    private fun stopSelfSafely() {
        try {
            sensorManager.unregisterListener(this)
            android.util.Log.d("StepTrackingService", "Unregistered sensor listener")
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "Error unregistering sensor: ${e.message}", e)
        }
        // Cancel timeout if still pending
        sensorReadTimeout?.removeCallbacksAndMessages(null)
        // Stop the service
        stopSelf()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "Error in onDestroy unregistering sensor: ${e.message}", e)
        }
        // Cancel timeout
        sensorReadTimeout?.removeCallbacksAndMessages(null)
        // Save final state
        userPreferences.setTotalStepCount(totalStepCount)
        userPreferences.setLastSensorValue(lastSensorValue)
    }
    
    companion object {
        private const val CHANNEL_ID = "StepTrackingChannel"
        private const val NOTIFICATION_ID = 1
    }
}

