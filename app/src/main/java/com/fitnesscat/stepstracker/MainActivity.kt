package com.fitnesscat.stepstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import android.app.PendingIntent
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    
    companion object {
        @Volatile
        var instance: MainActivity? = null
            private set
    }
    
    lateinit var userPreferences: UserPreferences
    lateinit var stepCounter: StepCounter
    lateinit var apiClient: ApiClient
    
    lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    
    private val PERMISSION_REQUEST_CODE = 1001
    private val ACTIVITY_RECOGNITION_REQUEST_CODE = 1002
    
    // Handlers for periodic updates
    private val mainHandler = Handler(Looper.getMainLooper())
    private var hourlySyncRunnable: Runnable? = null
    
    // Update intervals
    private val HOURLY_SYNC_INTERVAL_MS = 60 * 60 * 1000L // Sync every 1 hour

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Hide ActionBar if it exists
        supportActionBar?.hide()
        
        // Set instance for ActivityRecognitionReceiver
        instance = this
        
        // Initialize helpers FIRST (needed for fragments)
        userPreferences = UserPreferences(this)
        stepCounter = StepCounter(this, userPreferences)
        apiClient = ApiClient()
        
        // Initialize ViewPager2 and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        
        // Set up ViewPager adapter
        val adapter = ViewPagerAdapter(this)
        viewPager.adapter = adapter
        
        // Connect TabLayout with ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Gato"
                1 -> "Leaderboard"
                2 -> "Personalización"
                3 -> "Dev"
                4 -> "Mis datos"
                else -> ""
            }
        }.attach()
        
        // Schedule periodic step reading (every 1 hour)
        schedulePeriodicStepReading()
        
        // Request permissions FIRST, then setup
        requestPermissions()
        
        // Request battery optimization exemption (important for Motorola and other restrictive devices)
        requestBatteryOptimizationExemption()
    }
    
    /**
     * Schedules StepWorker to run every 1 hour to save step records to .txt file
     * Uses minimal constraints to work on restrictive devices like Motorola
     */
    private fun schedulePeriodicStepReading() {
        // Minimal constraints - allow work even when device is idle/charging
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED) // Can run offline
            .setRequiresBatteryNotLow(false) // Allow even when battery is low
            .setRequiresCharging(false) // Don't require charging
            .setRequiresDeviceIdle(false) // Allow even when device is active
            .build()
        
        val periodicWork = PeriodicWorkRequestBuilder<StepWorker>(
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour
            .build()
        
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StepWorker",
            ExistingPeriodicWorkPolicy.UPDATE, // Update existing work with new constraints
            periodicWork
        )
        
        android.util.Log.d("MainActivity", "Scheduled StepWorker to run every 1 hour with minimal constraints")
    }
    
    /**
     * Requests user to disable battery optimization for this app
     * This is critical for background step tracking on Motorola and other restrictive devices
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            val packageName = packageName
            
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    android.util.Log.d("MainActivity", "Requested battery optimization exemption")
                } catch (e: Exception) {
                    // Fallback: open battery settings manually
                    try {
                        val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        startActivity(intent)
                        android.util.Log.d("MainActivity", "Opened battery optimization settings")
                    } catch (e2: Exception) {
                        android.util.Log.e("MainActivity", "Could not open battery settings: ${e2.message}")
                    }
                }
            } else {
                android.util.Log.d("MainActivity", "Battery optimization already disabled")
            }
        }
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
        
        // Location permissions for GPS
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
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
            // All permissions already granted, start service
            android.util.Log.d("MainActivity", "All permissions already granted - starting service")
            startStepTrackingService()
            
            // Also retry after 2 seconds to ensure service starts (in case of timing issues)
            mainHandler.postDelayed({
                android.util.Log.d("MainActivity", "Retrying service start after 2 seconds (already had permissions)...")
                startStepTrackingService()
            }, 2000)
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
                android.util.Log.d("MainActivity", "All permissions granted - starting service")
                // Try to start service immediately
                startStepTrackingService()
                
                // Also retry after 2 seconds to ensure service starts (in case of timing issues)
                mainHandler.postDelayed({
                    android.util.Log.d("MainActivity", "Retrying service start after 2 seconds...")
                    startStepTrackingService()
                }, 2000)
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    /**
     * Starts the step tracking service
     * Made public so fragments can trigger service restart if needed
     */
    fun startStepTrackingService() {
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
            
            // Check notification permission for Android 13+ (required for foreground service)
            val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true // Permission not needed on older versions
            }
            
            if (!hasActivityRecognition) {
                // Permission not granted, don't start service
                android.util.Log.w("MainActivity", "Activity Recognition permission not granted - cannot start service")
                return
            }
            
            if (!hasNotificationPermission) {
                // Notification permission not granted (required for foreground service on Android 13+)
                android.util.Log.w("MainActivity", "POST_NOTIFICATIONS permission not granted - cannot start foreground service")
                return
            }
            
            android.util.Log.d("MainActivity", "Starting StepTrackingService...")
            AppLogger.log("MainActivity", "Starting StepTrackingService...")
            AppLogger.log("MainActivity", "Permissions - ActivityRecognition: $hasActivityRecognition, Notifications: $hasNotificationPermission")
            
            val serviceIntent = Intent(this, StepTrackingService::class.java)
            
            // Use startForegroundService since the service calls startForeground()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
                android.util.Log.d("MainActivity", "Called startForegroundService()")
                AppLogger.log("MainActivity", "Called startForegroundService()")
            } else {
                startService(serviceIntent)
                android.util.Log.d("MainActivity", "Called startService() (pre-Android O)")
                AppLogger.log("MainActivity", "Called startService() (pre-Android O)")
            }
            android.util.Log.d("MainActivity", "Service start requested")
            AppLogger.log("MainActivity", "Service start requested")
        } catch (e: SecurityException) {
            // Permission denied or service type not allowed
            android.util.Log.e("MainActivity", "SecurityException starting service: ${e.message}", e)
            AppLogger.log("MainActivity", "✗ SecurityException: ${e.message}")
            AppLogger.log("MainActivity", "Stack: ${e.stackTraceToString().take(200)}")
            Toast.makeText(this, "Cannot start step tracking: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: IllegalStateException) {
            // Service cannot be started (might already be starting)
            android.util.Log.e("MainActivity", "IllegalStateException starting service: ${e.message}", e)
            AppLogger.log("MainActivity", "✗ IllegalStateException: ${e.message}")
            AppLogger.log("MainActivity", "Stack: ${e.stackTraceToString().take(200)}")
        } catch (e: Exception) {
            // Other errors
            android.util.Log.e("MainActivity", "Error starting service: ${e.message}", e)
            AppLogger.log("MainActivity", "✗ Exception: ${e.javaClass.simpleName}: ${e.message}")
            AppLogger.log("MainActivity", "Stack: ${e.stackTraceToString().take(200)}")
            Toast.makeText(this, "Error starting service: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        android.util.Log.d("MainActivity", "onResume() - checking service")
        
        // Always try to start service (in case it stopped)
        startStepTrackingService()
        
        // Start hourly automatic sync (only while app is open)
        startHourlySync()
        
        // Note: Step files are saved hourly by StepWorker, can be exported via Dev tab
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
        
        // Get current GPS location
        val locationHelper = LocationHelper(this)
        locationHelper.getCurrentLocation { latitude, longitude ->
            // Sync to API (no rate limiting)
            apiClient.syncSteps(
                userId = userId,
                stepCount = stepCount,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
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
    }
    
    /**
     * Forces a sync to API Gateway endpoint (bypasses rate limiting)
     * Used for manual testing via button click
     * Made public so fragments can call it
     */
    fun forceSyncToAPI() {
        android.util.Log.d("MainActivity", "Manual sync triggered by button")
        
        // Get current data
        val userId = userPreferences.getUserId()
        val stepCount = userPreferences.getTotalStepCount()
        val timestamp = System.currentTimeMillis()
        
        // Show loading toast
        Toast.makeText(this, "Syncing to API...", Toast.LENGTH_SHORT).show()
        
        // Get current GPS location
        val locationHelper = LocationHelper(this)
        locationHelper.getCurrentLocation { latitude, longitude ->
            // Sync to API (bypass rate limiting)
            apiClient.syncSteps(
                userId = userId,
                stepCount = stepCount,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
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
    }
    
    /**
     * Syncs pending batch records to API when app opens
     * Reads all locally stored step records and sends them in batch
     */
    private fun syncPendingBatchRecords() {
        try {
            val recordsJson = userPreferences.getPendingStepRecords()
            android.util.Log.d("MainActivity", "Pending records JSON: $recordsJson")
            
            if (recordsJson.isEmpty() || recordsJson == "[]") {
                android.util.Log.d("MainActivity", "No pending records to sync")
                return
            }
            
            val userId = userPreferences.getUserId()
            android.util.Log.d("MainActivity", "Syncing batch for user: $userId")
            
            // Send batch to API (pasa el string directamente)
            apiClient.syncStepsBatch(
                userId = userId,
                recordsJsonString = recordsJson,
                callback = { success, errorMessage ->
                    runOnUiThread {
                        if (success) {
                            // Clear pending records on success
                            userPreferences.clearPendingStepRecords()
                            android.util.Log.d("MainActivity", "✓ Successfully synced batch")
                            Toast.makeText(
                                this,
                                "✓ Synced batch",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // Keep records for retry
                            android.util.Log.e("MainActivity", "✗ Failed to sync batch: $errorMessage")
                            Toast.makeText(
                                this,
                                "Batch sync failed: ${errorMessage?.take(80) ?: "Unknown error"}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            )
            
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "Error syncing batch records: ${e.message}", e)
            Toast.makeText(
                this,
                "Error: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
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
        
        // Get current GPS location
        val locationHelper = LocationHelper(this)
        locationHelper.getCurrentLocation { latitude, longitude ->
            // Sync to API
            apiClient.syncSteps(
                userId = userId,
                stepCount = stepCount,
                timestamp = timestamp,
                latitude = latitude,
                longitude = longitude,
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
    }

    override fun onPause() {
        super.onPause()
        // Stop hourly sync when app goes to background (service continues counting steps)
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        android.util.Log.d("MainActivity", "Stopped hourly sync (app paused)")
        android.util.Log.d("MainActivity", "Service continues running in background to count steps")
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
        hourlySyncRunnable?.let { mainHandler.removeCallbacks(it) }
        
        // Clear instance
        instance = null
        
        android.util.Log.d("MainActivity", "Cleaned up handlers (app destroyed)")
        // Service continues running in background to track steps
    }
}

