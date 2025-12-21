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
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONArray
import java.util.concurrent.TimeUnit
class MainActivity : AppCompatActivity() {
    
    private lateinit var stepsText: TextView
    private lateinit var syncButton: Button
    private lateinit var debugStatusText: TextView
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var stepCounter: StepCounter
    private lateinit var apiClient: ApiClient
    
    private val PERMISSION_REQUEST_CODE = 1001
    
    // Handlers for periodic updates
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stepUpdateRunnable: Runnable? = null
    private var hourlySyncRunnable: Runnable? = null
    
    // Update intervals
    private val STEP_UPDATE_INTERVAL_MS = 2000L // Update step count every 2 seconds
    private val HOURLY_SYNC_INTERVAL_MS = 60 * 60 * 1000L // Sync every 1 hour

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
        
        // Load and display initial data (before permissions)
        loadInitialData()
        
        // Schedule periodic step reading (every 30 minutes)
        schedulePeriodicStepReading()
        
        // Request permissions FIRST, then setup
        requestPermissions()
    }
    
    /**
     * Schedules StepWorker to run every 30 minutes to save step records
     */
    private fun schedulePeriodicStepReading() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Can run offline
            .build()
        
        val periodicWork = PeriodicWorkRequestBuilder<StepWorker>(
            30, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StepWorker",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            periodicWork
        )
        
        android.util.Log.d("MainActivity", "Scheduled StepWorker to run every 30 minutes")
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
            
            // Use startForegroundService since the service calls startForeground()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
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
        // Start StepCounter listener to update steps when app is open
        // This ensures steps are updated in real-time while app is running
        stepCounter.onStepCountChanged = { newStepCount ->
            runOnUiThread {
                stepsText.text = newStepCount.toString()
                android.util.Log.d("MainActivity", "Step count updated from StepCounter: $newStepCount")
            }
        }
        stepCounter.startListening()
        android.util.Log.d("MainActivity", "StepCounter started - will track steps while app is open")
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
        
        // Restart StepCounter listener to track steps while app is open
        if (::stepCounter.isInitialized) {
            stepCounter.startListening()
            android.util.Log.d("MainActivity", "Restarted StepCounter listener")
        }
        
        // Refresh step count immediately
        refreshStepCount()
        
        // Wait a bit for sensor to fire initial event, then refresh again
        mainHandler.postDelayed({
            refreshStepCount()
            android.util.Log.d("MainActivity", "Refreshed step count after sensor initialization")
        }, 500)
        
        // Start periodic step count updates (every 2 seconds)
        startStepCountUpdates()
        
        // Start hourly automatic sync (only while app is open)
        startHourlySync()
        
        // Sync pending batch records when app opens
        syncPendingBatchRecords()
        
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
     * Starts hourly automatic sync to API (only while app is open)
     */
    private fun startHourlySync() {
        // Cancel any existing sync runnable
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        
        // Create new runnable that syncs and schedules itself again
        hourlySyncRunnable = object : Runnable {
            override fun run() {
                android.util.Log.d("MainActivity", "Hourly sync triggered")
                syncStepsToAPI()
                // Schedule next hourly sync (only if app is still active)
                hourlySyncRunnable?.let { mainHandler.postDelayed(it, HOURLY_SYNC_INTERVAL_MS) }
            }
        }
        
        // Start the hourly sync (only runs while app is open)
        hourlySyncRunnable?.let { mainHandler.postDelayed(it, HOURLY_SYNC_INTERVAL_MS) }
        android.util.Log.d("MainActivity", "Started hourly automatic sync (every ${HOURLY_SYNC_INTERVAL_MS}ms) - only while app is open")
    }
    
    /**
     * Syncs steps to API when app closes (no rate limiting)
     * Uses runOnUiThread for callback to ensure proper thread handling
     */
    private fun syncStepsToAPIOnResume() {
        android.util.Log.d("MainActivity", "Syncing steps before app closes")
        
        // Get current data
        val userId = userPreferences.getUserId()
        val stepCount = userPreferences.getTotalStepCount()
        val timestamp = System.currentTimeMillis()
        
        // Sync to API (no rate limiting)
        apiClient.syncSteps(
            userId = userId,
            stepCount = stepCount,
            timestamp = timestamp,
            callback = { success, errorMessage ->
                runOnUiThread {
                    if (success) {
                        // Update last sync timestamp on success
                        userPreferences.setLastSyncTimestamp(timestamp)
                        android.util.Log.d("MainActivity", "✓ Successfully synced $stepCount steps on app close")
                    } else {
                        // Log error (user won't see it if app is closing, but helps debugging)
                        android.util.Log.e("MainActivity", "✗ Failed to sync steps on app close: $errorMessage")
                    }
                }
            }
        )
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
     * Syncs pending batch records to API when app opens
     * Reads all locally stored step records and sends them in batch
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
            
            // Send batch to API
            apiClient.syncStepsBatch(
                userId = userId,
                records = records,
                callback = { success, errorMessage ->
                    runOnUiThread {
                        if (success) {
                            // Clear pending records on success
                            userPreferences.clearPendingStepRecords()
                            android.util.Log.d("MainActivity", "✓ Successfully synced ${records.length()} records in batch")
                            Toast.makeText(
                                this,
                                "✓ Synced ${records.length()} records",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Keep records for retry
                            android.util.Log.e("MainActivity", "✗ Failed to sync batch: $errorMessage")
                            Toast.makeText(
                                this,
                                "Batch sync failed: ${errorMessage?.take(50) ?: "Unknown error"}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            )
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error syncing batch records: ${e.message}", e)
        }
    }
    
    /**
     * Syncs step count to API Gateway endpoint
     * Rate limiting removed - hourly timer already controls frequency
     */
    private fun syncStepsToAPI() {
        android.util.Log.d("MainActivity", "Automatic sync triggered (hourly)")
        
        // Get current data
        val userId = userPreferences.getUserId()
        val stepCount = userPreferences.getTotalStepCount()
        val timestamp = System.currentTimeMillis()
        
        // Sync to API
        apiClient.syncSteps(
            userId = userId,
            stepCount = stepCount,
            timestamp = timestamp,
            callback = { success, errorMessage ->
                runOnUiThread {
                    if (success) {
                        // Update last sync timestamp on success
                        userPreferences.setLastSyncTimestamp(timestamp)
                        android.util.Log.d("MainActivity", "✓ Successfully synced $stepCount steps (automatic)")
                    } else {
                        // Show error to user so they know sync failed
                        android.util.Log.e("MainActivity", "✗ Failed to sync steps (automatic): $errorMessage")
                        Toast.makeText(
                            this,
                            "Auto-sync failed: ${errorMessage?.take(50) ?: "Unknown error"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
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
        // Stop StepCounter listener when app goes to background (service handles it)
        stepCounter.stopListening()
        android.util.Log.d("MainActivity", "Stopped UI updates, hourly sync, and StepCounter (app paused)")
        
        android.util.Log.d("MainActivity", "Service continues running in background to count steps")
        // Service continues running in background to track steps
    }
    
    override fun onStop() {
        super.onStop()
        // Sync steps to API when app goes to background
        // onStop() is called later than onPause(), giving more time for sync to complete
        android.util.Log.d("MainActivity", "App stopped - syncing steps before background")
        syncStepsToAPIOnResume()
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

