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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import android.Manifest
import android.content.Intent
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevFragment : Fragment() {
    
    private lateinit var devStatusText: TextView
    private lateinit var devLogsText: TextView
    private lateinit var saveLogsButton: Button
    
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
        devLogsText = view.findViewById(R.id.devLogsText)
        saveLogsButton = view.findViewById(R.id.saveLogsButton)
        
        // Set up log listener to receive logs from AppLogger
        AppLogger.setLogListener { logLine ->
            updateLogsDisplay()
        }
        
        // Set up save logs button
        saveLogsButton.setOnClickListener {
            saveLogsToFile()
        }
        
        // Add initial log
        addLog("DevFragment", "Fragment created")
        
        // Load existing logs
        updateLogsDisplay()
        
        // Wait a bit before first check to give service time to start (avoid race condition)
        mainHandler.postDelayed({
            addLog("DevFragment", "Starting initial status check...")
            updateDebugStatus()
            // Start periodic updates after initial check
            startStatusUpdates()
        }, 500) // 500ms delay to let service appear in running services list
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Remove log listener when fragment is destroyed
        AppLogger.setLogListener(null)
    }
    
    private fun updateLogsDisplay() {
        mainHandler.post {
            devLogsText.text = AppLogger.getLogs()
            // Auto-scroll to bottom
            devLogsText.post {
                val scrollView = devLogsText.parent?.parent as? android.widget.ScrollView
                scrollView?.fullScroll(android.view.View.FOCUS_DOWN)
            }
        }
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
                updateLogsDisplay() // Also update logs display periodically
                statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
            }
        }
        
        statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
    }
    
    private fun addLog(tag: String, message: String) {
        AppLogger.log(tag, message)
        updateLogsDisplay()
    }
    
    private fun updateDebugStatus() {
        val context = requireContext()
        val statusMessages = mutableListOf<String>()
        
        addLog("DevFragment", "Updating debug status...")
        
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
            addLog("DevFragment", "Activity Recognition: ✓ Granted")
        } else {
            statusMessages.add("✗ Activity Recognition: DENIED")
            addLog("DevFragment", "Activity Recognition: ✗ DENIED")
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission) {
                statusMessages.add("✓ Notifications: Granted")
                addLog("DevFragment", "Notifications: ✓ Granted")
            } else {
                statusMessages.add("✗ Notifications: DENIED (needed for service)")
                addLog("DevFragment", "Notifications: ✗ DENIED")
            }
        }
        
        // Check sensor availability
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepCounterSensor != null) {
            statusMessages.add("✓ Sensor: Available")
            addLog("DevFragment", "Sensor: ✓ Available")
        } else {
            statusMessages.add("✗ Sensor: NOT AVAILABLE")
            addLog("DevFragment", "Sensor: ✗ NOT AVAILABLE")
        }
        
        // Check service status - use notification manager to check if foreground service is running
        // getRunningServices() is deprecated, so we check notifications instead for foreground services
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        
        // Check for our service notification (ID 1, no tag)
        var serviceRunning = false
        var notificationFound = false
        
        activeNotifications.forEach { notification ->
            android.util.Log.d("DevFragment", "Found notification: ID=${notification.id}, Tag=${notification.tag}, Channel=${notification.notification.channelId}")
            if (notification.id == 1 && notification.tag == null) {
                notificationFound = true
                // Also check if it's from our app by checking the channel ID
                if (notification.notification.channelId == "StepTrackingChannel" || 
                    notification.notification.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()?.contains("Step") == true) {
                    serviceRunning = true
                    android.util.Log.d("DevFragment", "✓ Service notification found!")
                }
            }
        }
        
        // If we found notification ID 1 but couldn't verify it's ours, still consider it running
        if (notificationFound && !serviceRunning) {
            serviceRunning = true
            android.util.Log.d("DevFragment", "Notification ID 1 found, assuming service is running")
        }
        
        // Fallback: Try deprecated method as last resort for older Android versions
        if (!serviceRunning && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            @Suppress("DEPRECATION")
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
            serviceRunning = runningServices.any {
                it.service.className == "com.fitnesscat.stepstracker.StepTrackingService"
            }
        }
        
        android.util.Log.d("DevFragment", "Service running check - notification based: $serviceRunning (found notification: $notificationFound, total notifications: ${activeNotifications.size})")
        addLog("DevFragment", "Service check: found=$serviceRunning, notifications=${activeNotifications.size}")
        
        if (serviceRunning) {
            statusMessages.add("✓ Service: Running")
            addLog("DevFragment", "Service: ✓ Running")
        } else {
            addLog("DevFragment", "Service: ✗ NOT RUNNING")
            statusMessages.add("✗ Service: NOT RUNNING")
            
            // Diagnostic information - why might it not be running?
            val mainActivity = activity as? MainActivity
            if (mainActivity != null && hasActivityRecognition && hasNotificationPermission) {
                // All permissions granted, but service not running - check why
                if (stepCounterSensor == null) {
                    statusMessages.add("⚠ Reason: Sensor NOT AVAILABLE")
                    statusMessages.add("   (Service needs sensor to run)")
                } else {
                    // All checks passed but service not running - likely failing during startup
                    statusMessages.add("⚠ Service may be crashing on start")
                    statusMessages.add("   Possible causes:")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        statusMessages.add("   - ForegroundServiceType issue (Android 14+)")
                    }
                    statusMessages.add("   - Exception in service onCreate()")
                    statusMessages.add("   Trying to restart...")
                }
                
                // Try to start the service
                try {
                    android.util.Log.d("DevFragment", "Service not running - attempting to start...")
                    addLog("DevFragment", "Attempting to start service...")
                    mainActivity.startStepTrackingService()
                    addLog("MainActivity", "startStepTrackingService() called")
                    
                    // Give it more time and check again (service needs time to fully initialize)
                    mainHandler.postDelayed({
                        val retryNotificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                        val retryActiveNotifications = retryNotificationManager.activeNotifications
                        
                        // Check if notification exists
                        val retryServiceRunning = retryActiveNotifications.any { notification ->
                            notification.id == 1 && notification.tag == null
                        }
                        
                        android.util.Log.d("DevFragment", "Retry check - Active notifications: ${retryActiveNotifications.size}")
                        retryActiveNotifications.forEach { notification ->
                            android.util.Log.d("DevFragment", "  Notification ID: ${notification.id}, Tag: ${notification.tag}")
                        }
                        
                        if (retryServiceRunning) {
                            android.util.Log.d("DevFragment", "✓ Service started successfully after retry")
                            addLog("DevFragment", "✓ Service started successfully!")
                            statusMessages.clear()
                            statusMessages.add("✓ Activity Recognition: Granted")
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                statusMessages.add("✓ Notifications: Granted")
                            }
                            statusMessages.add("✓ Sensor: Available")
                            statusMessages.add("✓ Service: Running")
                            updateDebugStatus()
                        } else {
                            android.util.Log.w("DevFragment", "Service still not running after start attempt")
                            addLog("DevFragment", "✗ Service still NOT running after retry")
                            statusMessages.add("")
                            statusMessages.add("⚠ Service failed to start")
                            statusMessages.add("   See logs below for details")
                            
                            // Check if there's a specific error
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                addLog("DevFragment", "Android ${Build.VERSION.SDK_INT} (Android 14+) detected")
                                statusMessages.add("")
                                statusMessages.add("Android ${Build.VERSION.SDK_INT} detected")
                                statusMessages.add("Check if foregroundServiceType='health'")
                                statusMessages.add("is in AndroidManifest.xml")
                            }
                            
                            updateDebugStatus()
                        }
                    }, 2000) // Increased to 2 seconds to give service more time
                } catch (e: SecurityException) {
                    android.util.Log.e("DevFragment", "SecurityException: ${e.message}", e)
                    addLog("DevFragment", "✗ SecurityException: ${e.message}")
                    statusMessages.add("⚠ Error: SecurityException")
                    statusMessages.add("   ${e.message}")
                } catch (e: IllegalStateException) {
                    android.util.Log.e("DevFragment", "IllegalStateException: ${e.message}", e)
                    addLog("DevFragment", "✗ IllegalStateException: ${e.message}")
                    statusMessages.add("⚠ Error: IllegalStateException")
                    statusMessages.add("   ${e.message}")
                } catch (e: Exception) {
                    android.util.Log.e("DevFragment", "Could not restart service: ${e.message}", e)
                    addLog("DevFragment", "✗ Exception: ${e.javaClass.simpleName}: ${e.message}")
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
    
    private fun saveLogsToFile() {
        try {
            val context = requireContext()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "fitness_cat_logs_$timestamp.txt"
            
            // Get logs content
            val statusText = devStatusText.text.toString()
            val logsText = AppLogger.getLogs()
            
            // Combine status and logs
            val fullContent = buildString {
                appendLine("=== Fitness Cat Debug Logs ===")
                appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
                appendLine("")
                appendLine("=== Status ===")
                appendLine(statusText)
                appendLine("")
                appendLine("=== Logs ===")
                appendLine(logsText)
            }
            
            // Save to cache directory (accessible and can be shared)
            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                writer.write(fullContent)
            }
            
            val filePath = file.absolutePath
            addLog("DevFragment", "✓ Logs saved to: $filePath")
            
            // Share the file using Intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fitness Cat Debug Logs")
                putExtra(Intent.EXTRA_TEXT, "Logs generados el ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                
                // For Android 7.0+ (API 24+), use FileProvider
                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    @Suppress("DEPRECATION")
                    android.net.Uri.fromFile(file)
                }
                
                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooserIntent = Intent.createChooser(shareIntent, "Compartir logs")
            startActivity(chooserIntent)
            
            Toast.makeText(
                context,
                "Archivo guardado. Elige cómo compartirlo.",
                Toast.LENGTH_SHORT
            ).show()
            
        } catch (e: Exception) {
            android.util.Log.e("DevFragment", "Error saving logs: ${e.message}", e)
            addLog("DevFragment", "✗ Error saving logs: ${e.message}")
            Toast.makeText(
                requireContext(),
                "Error al guardar logs: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}

