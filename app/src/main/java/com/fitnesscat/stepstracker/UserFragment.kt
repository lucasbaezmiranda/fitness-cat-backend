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
    
    // Current stage (1-5), derived from health — not set manually
    private var currentStage = 1

    // Cat code derived from 4 independent characteristics
    private var catCode = "0000"

    // Current health (0 to 80)
    private var currentHealth = UserPreferences.INITIAL_HEALTH
    private val MIN_HEALTH = 0
    private val MAX_HEALTH = UserPreferences.MAX_HEALTH
    
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
            currentHealth = mainActivity.userPreferences.getCurrentHealth()
            catCode = mainActivity.userPreferences.getCatCode()
            currentStage = mainActivity.userPreferences.healthToStage(currentHealth)
        }

        // Stage is derived from health — hide manual stage buttons
        prevStageButton.visibility = View.GONE
        nextStageButton.visibility = View.GONE

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
        // Reload cat code and health in case they changed
        mainActivity?.let {
            catCode = it.userPreferences.getCatCode()
            currentHealth = it.userPreferences.getCurrentHealth()
            currentStage = it.userPreferences.healthToStage(currentHealth)
        }
        updateBackground()
        refreshStepCount(mainActivity)
        startStepCountUpdates(mainActivity)
        updateStageImage()
        updateHealthBar()
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
    
    private fun updateStageImage() {
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            catCode = it.userPreferences.getCatCode()
        }

        val drawableName = if (currentStage == 1) "cat_stage_1"
                           else "cat_${catCode}_stage_${currentStage}"
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        stageImageView.setImageResource(if (resId != 0) resId else R.drawable.cat_stage_1)
    }
    
    private fun updateHealthBar() {
        healthProgressBar.max = MAX_HEALTH
        healthProgressBar.progress = currentHealth
        healthValueText.text = currentHealth.toString()

        // Colors aligned with stage ranges (max=80)
        val colorResId = when {
            currentHealth > 60 -> 0xFF4CAF50.toInt()  // Green  (stage 5: 61-80)
            currentHealth > 40 -> 0xFFFFEB3B.toInt()  // Yellow (stage 4: 41-60)
            currentHealth > 20 -> 0xFFFF9800.toInt()  // Orange (stage 3: 21-40)
            else               -> 0xFFF44336.toInt()  // Red    (stages 1-2: 0-20)
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

