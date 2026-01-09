package com.fitnesscat.stepstracker

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
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
        
        // Check service status - try multiple methods
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        
        // Check by class name
        var serviceRunning = runningServices.any {
            it.service.className == "com.fitnesscat.stepstracker.StepTrackingService"
        }
        
        // Also check by package name + service name
        if (!serviceRunning) {
            serviceRunning = runningServices.any {
                it.service.packageName == context.packageName && 
                it.service.className.contains("StepTrackingService")
            }
        }
        
        // Log all running services for debugging
        android.util.Log.d("DevFragment", "Total running services: ${runningServices.size}")
        runningServices.take(10).forEach { service ->
            android.util.Log.d("DevFragment", "Running service: ${service.service.packageName}.${service.service.className}")
        }
        
        if (serviceRunning) {
            statusMessages.add("✓ Service: Running")
        } else {
            statusMessages.add("✗ Service: NOT RUNNING")
            // Try to start the service if it's not running
            val mainActivity = activity as? MainActivity
            if (mainActivity != null) {
                try {
                    android.util.Log.d("DevFragment", "Service not running - attempting to start...")
                    mainActivity.startStepTrackingService()
                    // Give it a moment and check again
                    mainHandler.postDelayed({
                        val retryRunningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
                        val retryServiceRunning = retryRunningServices.any {
                            it.service.className == "com.fitnesscat.stepstracker.StepTrackingService"
                        }
                        if (retryServiceRunning) {
                            android.util.Log.d("DevFragment", "Service started successfully after retry")
                            updateDebugStatus()
                        } else {
                            android.util.Log.w("DevFragment", "Service still not running after start attempt")
                            statusMessages.add("⚠ Service start attempted but failed")
                        }
                    }, 1000)
                } catch (e: Exception) {
                    android.util.Log.e("DevFragment", "Could not restart service: ${e.message}", e)
                    statusMessages.add("⚠ Error: ${e.message}")
                }
            }
        }
        
        // Get last sensor value
        val mainActivity = activity as? MainActivity
        val lastSensorValue = mainActivity?.userPreferences?.getLastSensorValue() ?: 0f
        statusMessages.add("Last Sensor: $lastSensorValue")
        
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

