package com.fitnesscat.stepstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
class MainActivity : AppCompatActivity() {
    
    private lateinit var stepsText: TextView
    private lateinit var syncButton: Button
    private lateinit var debugStatusText: TextView
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var stepCounter: StepCounter
    private lateinit var apiClient: ApiClient
    
    private val PERMISSION_REQUEST_CODE = 1001
    
    // Rate limiting: minimum time between syncs (5 minutes)
    private val MIN_SYNC_INTERVAL_MS = 5 * 60 * 1000L
    
    // Handlers for periodic updates
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stepUpdateRunnable: Runnable? = null
    private var hourlySyncRunnable: Runnable? = null
    
    // Update intervals
    private val STEP_UPDATE_INTERVAL_MS = 2000L // Update step count every 2 seconds
    private val BATCH_SYNC_INTERVAL_MS = 2 * 60 * 60 * 1000L // Batch sync every 2 hours

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        stepsText = findViewById(R.id.stepsText)
        syncButton = findViewById(R.id.syncButton)
        debugStatusText = findViewById(R.id.debugStatusText)
        
        // Set up sync button click listener
        syncButton.setOnClickListener {
            forceSyncToAPI()
        }
        
        // Initialize helpers
        userPreferences = UserPreferences(this)
        stepCounter = StepCounter(this, userPreferences)
        apiClient = ApiClient()
        
        // Schedule WorkManager for periodic step reading (every 15 minutes)
        schedulePeriodicStepReading()
        
        // Load and display initial data (before permissions)
        loadInitialData()
        
        // Request permissions FIRST, then setup
        requestPermissions()
    }
    
    /**
     * Schedules WorkManager to read steps every 30 minutes
     * This runs in background without notification - most efficient solution
     */
    private fun schedulePeriodicStepReading() {
        val workRequest = PeriodicWorkRequestBuilder<StepWorker>(
            30, TimeUnit.MINUTES  // Every 30 minutes for optimal battery usage
        ).build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StepReadingWork",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
        
        android.util.Log.d("MainActivity", "Scheduled periodic step reading every 30 minutes")
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        
        // Activity Recognition permission (for step tracking)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
            }
        }
        
        // Notification permission (for foreground service on Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                permissionsToRequest.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        } else {
            // All permissions already granted, setup and start service
            setupStepCounter()
            startStepTrackingService()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            var allGranted = true
            for (i in grantResults.indices) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false
                    break
                }
            }
            
            if (allGranted) {
                setupStepCounter()
                startStepTrackingService()
                updateDebugStatus()
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_LONG
                ).show()
                updateDebugStatus()
            }
        }
    }
    
    private fun startStepTrackingService() {
        try {
            // Check permissions first
            val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not needed on older versions
            }
            
            if (!hasActivityRecognition) {
                // Permission not granted, don't start service
                android.util.Log.w("MainActivity", "Activity Recognition permission not granted - cannot start service")
                return
            }
            
            android.util.Log.d("MainActivity", "Starting StepTrackingService...")
            val serviceIntent = Intent(this, StepTrackingService::class.java)
            
            // Use regular startService since we're not using foreground service anymore
            startService(serviceIntent)
            android.util.Log.d("MainActivity", "Started service")
        } catch (e: SecurityException) {
            // Permission denied or service type not allowed
            android.util.Log.e("MainActivity", "Failed to start service: ${e.message}", e)
            Toast.makeText(this, "Cannot start step tracking: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Other errors
            android.util.Log.e("MainActivity", "Error starting service: ${e.message}", e)
            Toast.makeText(this, "Error starting service", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStepCounter() {
        // Don't start StepCounter listener - the service handles tracking
        // Just set up UI update callback for when service updates steps
        // The service updates UserPreferences, so we'll refresh from there
        android.util.Log.d("MainActivity", "StepCounter setup - service will handle tracking")
    }

    private fun loadInitialData() {
        // Display current step count from preferences (service updates this)
        android.util.Log.d("MainActivity", "Loading initial data...")
        val userId = userPreferences.getUserId()
        val currentSteps = userPreferences.getTotalStepCount()
        val lastSensorValue = userPreferences.getLastSensorValue()
        android.util.Log.d("MainActivity", "User ID: $userId")
        android.util.Log.d("MainActivity", "Current step count: $currentSteps")
        android.util.Log.d("MainActivity", "Last sensor value: $lastSensorValue")
        refreshStepCount()
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume() - refreshing step count and checking service")
        
        // Always try to start service (in case it stopped)
        startStepTrackingService()
        
        // Refresh step count immediately
        refreshStepCount()
        
        // Wait a bit for sensor to fire initial event, then refresh again
        mainHandler.postDelayed({
            refreshStepCount()
            android.util.Log.d("MainActivity", "Refreshed step count after sensor initialization")
        }, 500)
        
        // Send pending batch records to API
        syncPendingBatchRecords()
        
        // Start periodic step count updates (every 2 seconds)
        startStepCountUpdates()
        
        // Start batch sync timer (every 2 hours while app is open)
        startBatchSyncTimer()
        
        // Update debug status periodically
        updateDebugStatus()
    }
    
    /**
     * Starts periodic step count updates while app is in foreground
     */
    private fun startStepCountUpdates() {
        // Cancel any existing update runnable
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        
        // Create new runnable that updates steps and schedules itself again
        stepUpdateRunnable = object : Runnable {
            override fun run() {
                refreshStepCount()
                updateDebugStatus()
                // Schedule next update
                stepUpdateRunnable?.let { mainHandler.postDelayed(it, STEP_UPDATE_INTERVAL_MS) }
            }
        }
        
        // Start the periodic updates
        stepUpdateRunnable?.let { mainHandler.postDelayed(it, STEP_UPDATE_INTERVAL_MS) }
        android.util.Log.d("MainActivity", "Started periodic step count updates (every ${STEP_UPDATE_INTERVAL_MS}ms)")
    }
    
    /**
     * Starts batch sync timer (every 2 hours while app is open)
     */
    private fun startBatchSyncTimer() {
        // Cancel any existing sync runnable
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        
        // Create new runnable that syncs batch and schedules itself again
        hourlySyncRunnable = object : Runnable {
            override fun run() {
                android.util.Log.d("MainActivity", "Batch sync timer triggered (every 2 hours)")
                syncPendingBatchRecords()
                // Schedule next batch sync (only if app is still active)
                hourlySyncRunnable?.let { mainHandler.postDelayed(it, BATCH_SYNC_INTERVAL_MS) }
            }
        }
        
        // Start the batch sync timer (only runs while app is open)
        hourlySyncRunnable?.let { mainHandler.postDelayed(it, BATCH_SYNC_INTERVAL_MS) }
        android.util.Log.d("MainActivity", "Started batch sync timer (every ${BATCH_SYNC_INTERVAL_MS}ms) - only while app is open")
    }
    
    /**
     * Syncs all pending step records in batch to API
     * Called when app opens and every 2 hours
     */
    private fun syncPendingBatchRecords() {
        try {
            val pendingJson = userPreferences.getPendingStepRecords()
            
            if (pendingJson.isEmpty() || pendingJson == "[]") {
                android.util.Log.d("MainActivity", "No pending records to sync")
                return
            }
            
            val records = JSONArray(pendingJson)
            val userId = userPreferences.getUserId()
            
            if (records.length() == 0) {
                android.util.Log.d("MainActivity", "No pending records to sync")
                return
            }
            
            android.util.Log.d("MainActivity", "Syncing ${records.length()} pending records in batch")
            
            // Send each record to API
            var successCount = 0
            var failCount = 0
            
            for (i in 0 until records.length()) {
                val record = records.getJSONObject(i)
                val steps = record.optInt("steps_at_time", 0)
                val timestamp = record.optLong("timestamp", 0)
                
                // Convert timestamp from seconds to milliseconds for API
                val timestampMs = timestamp * 1000
                
                // Sync each record individually
                apiClient.syncSteps(
                    userId = userId,
                    stepCount = steps,
                    timestamp = timestampMs,
                    callback = { success, errorMessage ->
                        if (success) {
                            successCount++
                        } else {
                            failCount++
                            android.util.Log.e("MainActivity", "Failed to sync record: $errorMessage")
                        }
                        
                        // If this is the last record, clear pending if all succeeded
                        if (successCount + failCount == records.length()) {
                            if (failCount == 0) {
                                // All succeeded - clear pending records
                                userPreferences.clearPendingStepRecords()
                                android.util.Log.d("MainActivity", "✓ All ${successCount} records synced successfully")
                            } else {
                                // Some failed - keep failed ones (simple approach: keep all for retry)
                                android.util.Log.w("MainActivity", "Some records failed: $successCount succeeded, $failCount failed")
                            }
                        }
                    }
                )
            }
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error syncing batch records: ${e.message}", e)
        }
    }
    
    
    private fun refreshStepCount() {
        // Read current step count from preferences (updated by service)
        val currentSteps = userPreferences.getTotalStepCount()
        val lastSensorValue = userPreferences.getLastSensorValue()
        stepsText.text = currentSteps.toString()
        android.util.Log.d("MainActivity", "Refreshed step count: $currentSteps (lastSensorValue: $lastSensorValue)")
        
        // Update debug status
        updateDebugStatus()
    }
    
    private fun updateDebugStatus() {
        val statusMessages = mutableListOf<String>()
        
        // Check permission
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        
        if (hasPermission) {
            statusMessages.add("✓ Permission: Granted")
        } else {
            statusMessages.add("✗ Permission: DENIED")
        }
        
        // Check sensor availability
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        if (stepCounterSensor != null) {
            statusMessages.add("✓ Sensor: Available")
        } else {
            statusMessages.add("✗ Sensor: NOT AVAILABLE")
        }
        
        // Check service status (check if notification exists)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val activeNotifications = notificationManager.activeNotifications
        val serviceRunningByNotification = activeNotifications.any { 
            it.id == 1 && it.notification.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.contains("Step Tracker") == true
        }
        
        // Also check if service is actually running
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        val serviceActuallyRunning = runningServices.any { 
            it.service.className == "com.fitnesscat.stepstracker.StepTrackingService"
        }
        
        val serviceRunning = serviceRunningByNotification || serviceActuallyRunning
        
        if (serviceRunning) {
            statusMessages.add("✓ Service: Running")
        } else {
            statusMessages.add("✗ Service: NOT RUNNING")
            // Try to start it
            startStepTrackingService()
        }
        
        // Show step count info
        val currentSteps = userPreferences.getTotalStepCount()
        val lastSensorValue = userPreferences.getLastSensorValue()
        statusMessages.add("Steps: $currentSteps")
        statusMessages.add("Last Sensor: $lastSensorValue")
        
        // Update UI
        debugStatusText.text = statusMessages.joinToString("\n")
        
        // Color code based on status
        if (!hasPermission || stepCounterSensor == null || !serviceRunning) {
            debugStatusText.setTextColor(0xFFFF0000.toInt()) // Red
        } else if (lastSensorValue == 0f) {
            debugStatusText.setTextColor(0xFFFF8800.toInt()) // Orange - waiting for sensor
        } else {
            debugStatusText.setTextColor(0xFF00AA00.toInt()) // Green - all good
        }
    }
    
    /**
     * Forces a sync to API Gateway endpoint (bypasses rate limiting)
     * Used for manual testing via button click
     */
    private fun forceSyncToAPI() {
        android.util.Log.d("MainActivity", "Manual sync triggered by button")
        
        // Get current data
        val userId = userPreferences.getUserId()
        val stepCount = userPreferences.getTotalStepCount()
        val timestamp = System.currentTimeMillis()
        
        // Show loading toast
        Toast.makeText(this, "Syncing to API...", Toast.LENGTH_SHORT).show()
        
        // Sync to API (bypass rate limiting)
        apiClient.syncSteps(
            userId = userId,
            stepCount = stepCount,
            timestamp = timestamp,
            callback = { success, errorMessage ->
                runOnUiThread {
                    if (success) {
                        // Update last sync timestamp on success
                        userPreferences.setLastSyncTimestamp(timestamp)
                        android.util.Log.d("MainActivity", "✓ Successfully synced $stepCount steps to API")
                        Toast.makeText(
                            this,
                            "✓ Synced successfully!\nSteps: $stepCount\nUser: ${userId.take(8)}...",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        // Show error to user
                        android.util.Log.e("MainActivity", "✗ Failed to sync steps: $errorMessage")
                        Toast.makeText(
                            this,
                            "✗ Sync failed: $errorMessage",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        )
    }
    
    /**
     * Syncs step count to API Gateway endpoint
     * Includes rate limiting to prevent excessive API calls
     */
    private fun syncStepsToAPI() {
        val lastSync = userPreferences.getLastSyncTimestamp()
        val now = System.currentTimeMillis()
        
        // Check if enough time has passed since last sync
        if (now - lastSync < MIN_SYNC_INTERVAL_MS) {
            android.util.Log.d("MainActivity", "Skipping sync - too soon since last sync")
            return
        }
        
        // Get current data
        val userId = userPreferences.getUserId()
        val stepCount = userPreferences.getTotalStepCount()
        val timestamp = now
        
        // Sync to API
        apiClient.syncSteps(
            userId = userId,
            stepCount = stepCount,
            timestamp = timestamp,
            callback = { success, errorMessage ->
                if (success) {
                    // Update last sync timestamp on success
                    userPreferences.setLastSyncTimestamp(timestamp)
                    android.util.Log.d("MainActivity", "Successfully synced $stepCount steps")
                } else {
                    // Log error but don't show to user (silent failure)
                    android.util.Log.e("MainActivity", "Failed to sync steps: $errorMessage")
                }
            }
        )
    }

    override fun onPause() {
        super.onPause()
        // Stop periodic UI updates when app goes to background
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        // Stop hourly sync when app goes to background (service continues counting steps)
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        android.util.Log.d("MainActivity", "Stopped UI updates and hourly sync (app paused)")
        
        // Sync pending batch records when app closes
        android.util.Log.d("MainActivity", "Syncing pending batch records before app closes")
        syncPendingBatchRecords()
        
        android.util.Log.d("MainActivity", "Service continues running in background to count steps")
        // Service continues running in background to track steps
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up handlers
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        android.util.Log.d("MainActivity", "Cleaned up handlers (app destroyed)")
        // Service continues running in background to track steps
    }
}

