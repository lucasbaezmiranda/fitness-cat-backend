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
    
    override fun onCreate() {
        super.onCreate()
        
        try {
            userPreferences = UserPreferences(this)
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
            stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            
            // Load saved state
            totalStepCount = userPreferences.getTotalStepCount()
            lastSensorValue = userPreferences.getLastSensorValue()
            
            // Create notification channel for Android O+
            createNotificationChannel()
            
            // Start as foreground service with health type
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    // Android 14+ requires explicit service type
                    startForeground(NOTIFICATION_ID, createNotification(), 
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH)
                } else {
                    // Older versions
                    startForeground(NOTIFICATION_ID, createNotification())
                }
            } catch (e: Exception) {
                android.util.Log.e("StepTrackingService", "Failed to start foreground: ${e.message}", e)
                // Try without service type for older versions
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, createNotification())
                } else {
                    throw e
                }
            }
            
            // Check permission before accessing sensor
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not needed on older versions
            }
            
            // Start listening to sensor (only if sensor is available and permission granted)
            if (hasPermission && stepCounterSensor != null) {
                android.util.Log.d("StepTrackingService", "Starting step tracking - permission: granted, sensor: available")
                android.util.Log.d("StepTrackingService", "Current state - totalSteps: $totalStepCount, lastSensorValue: $lastSensorValue")
                startStepTracking()
            } else {
                if (!hasPermission) {
                    android.util.Log.w("StepTrackingService", "Activity Recognition permission not granted - stopping service")
                    // Stop service if permission not granted
                    stopSelf()
                    return
                }
                if (stepCounterSensor == null) {
                    android.util.Log.w("StepTrackingService", "Step counter sensor not available - stopping service")
                    // List all available sensors for debugging
                    val sensorList = sensorManager.getSensorList(Sensor.TYPE_ALL)
                    android.util.Log.d("StepTrackingService", "Available sensors: ${sensorList.map { "${it.name} (type=${it.type})" }}")
                    stopSelf()
                    return
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "Error in onCreate: ${e.message}", e)
            // Stop the service if initialization fails
            stopSelf()
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Restart service if killed
        return START_STICKY
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
                val registered = sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
                if (registered) {
                    android.util.Log.d("StepTrackingService", "Successfully registered step counter sensor listener")
                    android.util.Log.d("StepTrackingService", "Sensor name: ${sensor.name}, vendor: ${sensor.vendor}, maxRange: ${sensor.maximumRange}")
                    android.util.Log.d("StepTrackingService", "Sensor resolution: ${sensor.resolution}, power: ${sensor.power}mA")
                } else {
                    android.util.Log.e("StepTrackingService", "Failed to register sensor listener")
                }
            } ?: run {
                android.util.Log.w("StepTrackingService", "Step counter sensor is null")
            }
        } catch (e: Exception) {
            android.util.Log.e("StepTrackingService", "Error starting step tracking: ${e.message}", e)
        }
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val currentSensorValue = event.values[0]
            android.util.Log.d("StepTrackingService", "Sensor event received: currentValue=$currentSensorValue, lastValue=$lastSensorValue, totalSteps=$totalStepCount")
            
            // First reading - just store baseline
            if (lastSensorValue == 0f) {
                android.util.Log.d("StepTrackingService", "First sensor reading - storing baseline: $currentSensorValue")
                lastSensorValue = currentSensorValue
                userPreferences.setLastSensorValue(lastSensorValue)
                updateNotification()
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
                updateNotification()
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
                
                android.util.Log.d("StepTrackingService", "Updated step count: +$stepsSinceLastReading steps. New total: $totalStepCount")
                
                // Update notification
                updateNotification()
            } else {
                android.util.Log.d("StepTrackingService", "No new steps detected (stepsSinceLastReading=$stepsSinceLastReading)")
            }
        } else {
            android.util.Log.w("StepTrackingService", "Received sensor event for wrong sensor type: ${event?.sensor?.type}")
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        // Save final state
        userPreferences.setTotalStepCount(totalStepCount)
        userPreferences.setLastSensorValue(lastSensorValue)
    }
    
    companion object {
        private const val CHANNEL_ID = "StepTrackingChannel"
        private const val NOTIFICATION_ID = 1
    }
}

