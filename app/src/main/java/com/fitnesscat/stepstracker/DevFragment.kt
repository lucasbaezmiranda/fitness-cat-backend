package com.fitnesscat.stepstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Build.VERSION
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.Manifest

class DevFragment : Fragment() {
    
    private lateinit var devStatusText: TextView
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var statusUpdateRunnable: Runnable? = null
    private val STATUS_UPDATE_INTERVAL_MS = 2000L // Update every 2 seconds
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dev, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        devStatusText = view.findViewById(R.id.devStatusText)
        
        // Wait a bit before first check to give service time to start (avoid race condition)
        mainHandler.postDelayed({
            updateDebugStatus()
            // Start periodic updates after initial check
            startStatusUpdates()
        }, 500) // 500ms delay to let service appear in running services list
    }
    
    override fun onResume() {
        super.onResume()
        updateDebugStatus()
        startStatusUpdates()
    }
    
    override fun onPause() {
        super.onPause()
        statusUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    private fun startStatusUpdates() {
        statusUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        
        statusUpdateRunnable = object : Runnable {
            override fun run() {
                updateDebugStatus()
                statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
            }
        }
        
        statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
    }
    
    private fun updateDebugStatus() {
        val context = requireContext()
        val statusMessages = mutableListOf<String>()
        
        // Check Activity Recognition permission
        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        // Check Notification permission (required for foreground service on Android 13+)
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        if (hasActivityRecognition) {
            statusMessages.add("✓ Activity Recognition: Granted")
        } else {
            statusMessages.add("✗ Activity Recognition: DENIED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission) {
                statusMessages.add("✓ Notifications: Granted")
            } else {
                statusMessages.add("✗ Notifications: DENIED (needed for service)")
            }
        }
        
        // Check sensor availability
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepCounterSensor != null) {
            statusMessages.add("✓ Sensor: Available")
        } else {
            statusMessages.add("✗ Sensor: NOT AVAILABLE")
        }
        
        // Check service status - use notification manager to check if foreground service is running
        // getRunningServices() is deprecated, so we check notifications instead for foreground services
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        var serviceRunning = activeNotifications.any { notification ->
            notification.id == 1 && notification.tag == null // Our service uses notification ID 1
        }
        
        // Fallback: Try deprecated method as last resort (suppress warning for now)
        if (!serviceRunning && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            serviceRunning = runningServices.any {
                it.service.className == "com.fitnesscat.stepstracker.StepTrackingService"
            }
        }
        
        android.util.Log.d("DevFragment", "Service running check - notification based: $serviceRunning")
        
        if (serviceRunning) {
            statusMessages.add("✓ Service: Running")
        } else {
            statusMessages.add("✗ Service: NOT RUNNING")
            
            // Diagnostic information - why might it not be running?
            val mainActivity = activity as? MainActivity
            if (mainActivity != null && hasActivityRecognition && hasNotificationPermission) {
                // All permissions granted, but service not running - check why
                if (stepCounterSensor == null) {
                    statusMessages.add("⚠ Reason: Sensor NOT AVAILABLE")
                    statusMessages.add("   (Service needs sensor to run)")
                } else {
                    statusMessages.add("⚠ Reason: Unknown (check logs)")
                    statusMessages.add("   Trying to restart...")
                }
                
                // Try to start the service
                try {
                    android.util.Log.d("DevFragment", "Service not running - attempting to start...")
                    mainActivity.startStepTrackingService()
                    
                    // Give it a moment and check again
                    mainHandler.postDelayed({
                        val retryNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val retryActiveNotifications = retryNotificationManager.activeNotifications
                        val retryServiceRunning = retryActiveNotifications.any { notification ->
                            notification.id == 1 && notification.tag == null
                        }
                        if (retryServiceRunning) {
                            android.util.Log.d("DevFragment", "Service started successfully after retry")
                            updateDebugStatus()
                        } else {
                            android.util.Log.w("DevFragment", "Service still not running after start attempt")
                            // Check again why it's not running
                            val retrySensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                            if (retrySensor == null) {
                                statusMessages.add("⚠ Still failing: Sensor unavailable")
                            } else {
                                statusMessages.add("⚠ Still failing: Check Android version")
                                statusMessages.add("   Android: ${Build.VERSION.SDK_INT}")
                                if (Build.VERSION.SDK_INT >= 34) {
                                    statusMessages.add("   (Android 14+ may need foregroundServiceType)")
                                }
                            }
                            updateDebugStatus()
                        }
                    }, 1500)
                } catch (e: SecurityException) {
                    android.util.Log.e("DevFragment", "SecurityException: ${e.message}", e)
                    statusMessages.add("⚠ Error: SecurityException")
                    statusMessages.add("   ${e.message}")
                } catch (e: IllegalStateException) {
                    android.util.Log.e("DevFragment", "IllegalStateException: ${e.message}", e)
                    statusMessages.add("⚠ Error: IllegalStateException")
                    statusMessages.add("   ${e.message}")
                } catch (e: Exception) {
                    android.util.Log.e("DevFragment", "Could not restart service: ${e.message}", e)
                    statusMessages.add("⚠ Error: ${e.javaClass.simpleName}")
                    statusMessages.add("   ${e.message}")
                }
            } else if (!hasActivityRecognition) {
                statusMessages.add("⚠ Reason: Missing Activity Recognition")
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                statusMessages.add("⚠ Reason: Missing Notification permission")
            }
        }
        
        // Get last sensor value
        val mainActivity = activity as? MainActivity
        val lastSensorValue = mainActivity?.userPreferences?.getLastSensorValue() ?: 0f
        statusMessages.add("Last Sensor: $lastSensorValue")
        
        // Add Android version info for debugging
        statusMessages.add("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        
        // Update UI
        devStatusText.text = statusMessages.joinToString("\n")
        
        // Color code based on status
        val hasAllPermissions = hasActivityRecognition && (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || hasNotificationPermission)
        if (!hasAllPermissions || stepCounterSensor == null || !serviceRunning) {
            devStatusText.setTextColor(0xFFFF0000.toInt()) // Red
        } else if (lastSensorValue == 0f) {
            devStatusText.setTextColor(0xFFFF8800.toInt()) // Orange - waiting for sensor
        } else {
            devStatusText.setTextColor(0xFF00AA00.toInt()) // Green - all good
        }
    }
}

