package com.fitnesscat.stepstracker

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class UserFragment : Fragment() {
    
    private lateinit var backgroundImageView: ImageView
    private lateinit var stepsText: TextView
    private lateinit var stageImageView: ImageView
    private lateinit var prevStageButton: Button
    private lateinit var nextStageButton: Button
    private lateinit var healthProgressBar: ProgressBar
    private lateinit var healthValueText: TextView
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stepUpdateRunnable: Runnable? = null
    
    // Current stage (1, 2, 3, 4, or 5)
    private var currentStage = 1
    private val MIN_STAGE = 1
    private val MAX_STAGE = 5
    
    // Cat code derived from 4 independent characteristics
    private var catCode = "0000"
    
    // Current health (0 to 100)
    private var currentHealth = 100
    private val MIN_HEALTH = 0
    private val MAX_HEALTH = 100
    
    // Update intervals
    private val STEP_UPDATE_INTERVAL_MS = 2000L // Update step count every 2 seconds
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        backgroundImageView = view.findViewById(R.id.backgroundImageView)
        stepsText = view.findViewById(R.id.stepsText)
        stageImageView = view.findViewById(R.id.stageImageView)
        prevStageButton = view.findViewById(R.id.prevStageButton)
        nextStageButton = view.findViewById(R.id.nextStageButton)
        healthProgressBar = view.findViewById(R.id.healthProgressBar)
        healthValueText = view.findViewById(R.id.healthValueText)
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved stage, health, and cat code
            currentStage = mainActivity.userPreferences.getCurrentStage()
            currentHealth = mainActivity.userPreferences.getCurrentHealth()
            catCode = mainActivity.userPreferences.getCatCode()
        }
        
        // Set up button listeners
        prevStageButton.setOnClickListener {
            changeStage(-1, mainActivity)
        }
        
        nextStageButton.setOnClickListener {
            changeStage(1, mainActivity)
        }
        
        // Update UI with loaded values
        updateStageImage()
        updateHealthBar()
        updateBackground()
        
        // Refresh step count
        refreshStepCount(mainActivity)
        
        // Start periodic step count updates
        startStepCountUpdates(mainActivity)
    }
    
    override fun onResume() {
        super.onResume()
        val mainActivity = activity as? MainActivity
        // Reload cat code in case it was changed in CustomizationFragment
        mainActivity?.let {
            catCode = it.userPreferences.getCatCode()
        }
        updateBackground() // Update background in case time changed
        refreshStepCount(mainActivity)
        startStepCountUpdates(mainActivity)
        updateStageImage() // Update stage image with current skin
    }
    
    override fun onPause() {
        super.onPause()
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    private fun refreshStepCount(mainActivity: MainActivity?) {
        mainActivity?.let {
            // Show only today's steps (not accumulated)
            val todaySteps = it.userPreferences.getTodayStepCount()
            val previousText = stepsText.text.toString()
            stepsText.text = todaySteps.toString()
            android.util.Log.d("UserFragment", "Refreshed today's step count: $todaySteps (was: $previousText)")
            AppLogger.log("UserFragment", "Reading today's steps: $todaySteps")
        }
    }
    
    private fun startStepCountUpdates(mainActivity: MainActivity?) {
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
        
        stepUpdateRunnable = object : Runnable {
            override fun run() {
                refreshStepCount(mainActivity)
                stepUpdateRunnable?.let { mainHandler.postDelayed(it, STEP_UPDATE_INTERVAL_MS) }
            }
        }
        
        stepUpdateRunnable?.let { mainHandler.postDelayed(it, STEP_UPDATE_INTERVAL_MS) }
    }
    
    private fun changeStage(delta: Int, mainActivity: MainActivity?) {
        val newStage = currentStage + delta
        
        if (newStage < MIN_STAGE || newStage > MAX_STAGE) {
            return
        }
        
        currentStage = newStage
        mainActivity?.userPreferences?.setCurrentStage(currentStage)
        updateStageImage()
        android.util.Log.d("UserFragment", "Changed stage to $currentStage")
    }
    
    private fun updateStageImage() {
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            catCode = it.userPreferences.getCatCode()
        }

        val drawableName = if (currentStage == 1) "cat_stage_1"
                           else "cat_${catCode}_stage_${currentStage}"
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        stageImageView.setImageResource(if (resId != 0) resId else R.drawable.cat_stage_1)

        prevStageButton.isEnabled = currentStage > MIN_STAGE
        nextStageButton.isEnabled = currentStage < MAX_STAGE
    }
    
    private fun changeHealth(delta: Int, mainActivity: MainActivity?) {
        val newHealth = currentHealth + delta
        
        if (newHealth < MIN_HEALTH || newHealth > MAX_HEALTH) {
            return
        }
        
        currentHealth = newHealth
        mainActivity?.userPreferences?.setCurrentHealth(currentHealth)
        updateHealthBar()
        android.util.Log.d("UserFragment", "Changed health to $currentHealth")
    }
    
    private fun updateHealthBar() {
        healthProgressBar.progress = currentHealth
        healthValueText.text = currentHealth.toString()
        
        val colorResId = when {
            currentHealth > 70 -> 0xFF4CAF50.toInt()  // Green
            currentHealth >= 50 -> 0xFFFFEB3B.toInt()  // Yellow
            currentHealth >= 25 -> 0xFFFF9800.toInt()  // Orange
            else -> 0xFFF44336.toInt()  // Red
        }
        
        healthProgressBar.progressTintList = ColorStateList.valueOf(colorResId)
    }
    
    /**
     * Updates the background image based on time of day
     * Day: 6 AM - 8 PM (background_day.jpg)
     * Night: 8 PM - 6 AM (background_night.jpg)
     */
    private fun updateBackground() {
        val calendar = java.util.Calendar.getInstance()
        val hour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        
        // Day: 6 AM to 8 PM (6-20), Night: 8 PM to 6 AM (20-6)
        val isDay = hour >= 6 && hour < 20
        
        val backgroundResId = if (isDay) {
            R.drawable.background_day
        } else {
            R.drawable.background_night
        }
        
        backgroundImageView.setImageResource(backgroundResId)
        android.util.Log.d("UserFragment", "Updated background: ${if (isDay) "Day" else "Night"} (hour: $hour)")
    }
}

