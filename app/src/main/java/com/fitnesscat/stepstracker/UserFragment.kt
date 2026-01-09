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
    
    private lateinit var stepsText: TextView
    private lateinit var syncButton: Button
    private lateinit var stageImageView: ImageView
    private lateinit var prevStageButton: Button
    private lateinit var nextStageButton: Button
    private lateinit var healthProgressBar: ProgressBar
    private lateinit var healthValueText: TextView
    private lateinit var increaseHealthButton: Button
    private lateinit var decreaseHealthButton: Button
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var stepUpdateRunnable: Runnable? = null
    
    // Current stage (1, 2, or 3)
    private var currentStage = 1
    private val MIN_STAGE = 1
    private val MAX_STAGE = 3
    
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
        stepsText = view.findViewById(R.id.stepsText)
        syncButton = view.findViewById(R.id.syncButton)
        stageImageView = view.findViewById(R.id.stageImageView)
        prevStageButton = view.findViewById(R.id.prevStageButton)
        nextStageButton = view.findViewById(R.id.nextStageButton)
        healthProgressBar = view.findViewById(R.id.healthProgressBar)
        healthValueText = view.findViewById(R.id.healthValueText)
        increaseHealthButton = view.findViewById(R.id.increaseHealthButton)
        decreaseHealthButton = view.findViewById(R.id.decreaseHealthButton)
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved stage and health
            currentStage = mainActivity.userPreferences.getCurrentStage()
            currentHealth = mainActivity.userPreferences.getCurrentHealth()
        }
        
        // Set up button listeners
        syncButton.setOnClickListener {
            mainActivity?.forceSyncToAPI()
        }
        
        prevStageButton.setOnClickListener {
            changeStage(-1, mainActivity)
        }
        
        nextStageButton.setOnClickListener {
            changeStage(1, mainActivity)
        }
        
        increaseHealthButton.setOnClickListener {
            changeHealth(1, mainActivity)
        }
        
        decreaseHealthButton.setOnClickListener {
            changeHealth(-1, mainActivity)
        }
        
        // Update UI with loaded values
        updateStageImage()
        updateHealthBar()
        
        // Refresh step count
        refreshStepCount(mainActivity)
        
        // Start periodic step count updates
        startStepCountUpdates(mainActivity)
    }
    
    override fun onResume() {
        super.onResume()
        val mainActivity = activity as? MainActivity
        refreshStepCount(mainActivity)
        startStepCountUpdates(mainActivity)
    }
    
    override fun onPause() {
        super.onPause()
        stepUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    private fun refreshStepCount(mainActivity: MainActivity?) {
        mainActivity?.let {
            val currentSteps = it.userPreferences.getTotalStepCount()
            val previousText = stepsText.text.toString()
            stepsText.text = currentSteps.toString()
            android.util.Log.d("UserFragment", "Refreshed step count: $currentSteps (was: $previousText)")
            AppLogger.log("UserFragment", "Reading steps: $currentSteps")
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
        val drawableResId = when (currentStage) {
            1 -> R.drawable.stage_1
            2 -> R.drawable.stage_2
            3 -> R.drawable.stage_3
            else -> R.drawable.stage_1
        }
        
        stageImageView.setImageResource(drawableResId)
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
        increaseHealthButton.isEnabled = currentHealth < MAX_HEALTH
        decreaseHealthButton.isEnabled = currentHealth > MIN_HEALTH
    }
}

