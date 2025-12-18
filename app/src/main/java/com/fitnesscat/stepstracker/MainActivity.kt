package com.fitnesscat.stepstracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
class MainActivity : AppCompatActivity() {
    
    private lateinit var stepsText: TextView
    
    private lateinit var userPreferences: UserPreferences
    private lateinit var stepCounter: StepCounter
    
    private val PERMISSION_REQUEST_CODE = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize views
        stepsText = findViewById(R.id.stepsText)
        
        // Initialize helpers
        userPreferences = UserPreferences(this)
        stepCounter = StepCounter(this, userPreferences)
        
        // Load and display initial data (before permissions)
        loadInitialData()
        
        // Request permissions FIRST, then setup
        requestPermissions()
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
            } else {
                Toast.makeText(
                    this,
                    getString(R.string.permission_denied),
                    Toast.LENGTH_LONG
                ).show()
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
                return
            }
            
            val serviceIntent = Intent(this, StepTrackingService::class.java)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: SecurityException) {
            // Permission denied or service type not allowed
            android.util.Log.e("MainActivity", "Failed to start service: ${e.message}")
            Toast.makeText(this, "Cannot start step tracking: ${e.message}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Other errors
            android.util.Log.e("MainActivity", "Error starting service: ${e.message}", e)
            Toast.makeText(this, "Error starting service", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupStepCounter() {
        stepCounter.onStepCountChanged = { count ->
            runOnUiThread {
                stepsText.text = count.toString()
            }
        }
        
        stepCounter.startListening()
    }

    private fun loadInitialData() {
        // Display current step count from preferences (service updates this)
        refreshStepCount()
    }

    override fun onResume() {
        super.onResume()
        // Refresh step count from service (service updates UserPreferences)
        refreshStepCount()
        stepCounter.startListening()
    }
    
    private fun refreshStepCount() {
        // Read current step count from preferences (updated by service)
        val currentSteps = userPreferences.getTotalStepCount()
        stepsText.text = currentSteps.toString()
    }

    override fun onPause() {
        super.onPause()
        // Don't stop listening - service handles background tracking
        // Just refresh display when we come back
    }

    override fun onDestroy() {
        super.onDestroy()
        // Service continues running in background
        // No need to stop stepCounter here
    }
}

