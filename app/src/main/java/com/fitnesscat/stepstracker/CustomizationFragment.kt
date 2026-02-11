package com.fitnesscat.stepstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class CustomizationFragment : Fragment() {
    
    private lateinit var catImageView: ImageView
    private lateinit var prevSkinButton: Button
    private lateinit var nextSkinButton: Button
    private lateinit var skinNameText: TextView
    private lateinit var selectButton: Button
    private lateinit var myDataButton: Button
    
    private var currentSkin = 0
    private val MIN_SKIN = 0
    private val MAX_SKIN = 3
    private val TOTAL_STAGES = 5
    
    // Current stage being displayed in preview (showing stage 3a as requested)
    private val PREVIEW_STAGE = 3
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customization, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        catImageView = view.findViewById(R.id.catImageView)
        prevSkinButton = view.findViewById(R.id.prevSkinButton)
        nextSkinButton = view.findViewById(R.id.nextSkinButton)
        skinNameText = view.findViewById(R.id.skinNameText)
        selectButton = view.findViewById(R.id.selectButton)
        myDataButton = view.findViewById(R.id.myDataButton)
        
        AppLogger.log("CustomizationFragment", "Views initialized")
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved skin
            currentSkin = mainActivity.userPreferences.getSelectedSkin()
            AppLogger.log("CustomizationFragment", "Loaded skin from preferences: $currentSkin")
            
            // If setup is already complete, disable skin selection buttons
            if (mainActivity.userPreferences.isInitialSetupComplete()) {
                prevSkinButton.isEnabled = false
                nextSkinButton.isEnabled = false
                selectButton.isEnabled = false
                selectButton.text = "Configuración completada"
                AppLogger.log("CustomizationFragment", "Setup already complete - buttons disabled")
            }
        } else {
            AppLogger.log("CustomizationFragment", "⚠ MainActivity is null!")
        }
        
        // Set up button listeners for carousel navigation
        prevSkinButton.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Prev button clicked, currentSkin: $currentSkin")
            changeSkin(-1)
        }
        
        nextSkinButton.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Next button clicked, currentSkin: $currentSkin")
            changeSkin(1)
        }
        
        selectButton.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Select button clicked")
            saveSelectedSkin(mainActivity)
        }
        
        myDataButton.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Mis datos button clicked")
            navigateToUserData()
        }
        
        // Update UI with loaded values
        updateCatImage()
        updateSkinInfo()
        updateButtonStates()
        
        AppLogger.log("CustomizationFragment", "✓ Fragment setup complete")
    }
    
    override fun onResume() {
        super.onResume()
        AppLogger.log("CustomizationFragment", "onResume called")
        
        // Reload skin in case it was changed elsewhere
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            val savedSkin = it.userPreferences.getSelectedSkin()
            if (savedSkin != currentSkin) {
                AppLogger.log("CustomizationFragment", "Skin changed externally: $currentSkin -> $savedSkin")
                currentSkin = savedSkin
                updateCatImage()
                updateSkinInfo()
                updateButtonStates()
            }
        }
    }
    
    private fun changeSkin(delta: Int) {
        val newSkin = currentSkin + delta
        
        AppLogger.log("CustomizationFragment", "changeSkin called: delta=$delta, currentSkin=$currentSkin, newSkin=$newSkin")
        
        // Circular carousel: wrap around
        val finalSkin = when {
            newSkin < MIN_SKIN -> MAX_SKIN // Go to last if before first
            newSkin > MAX_SKIN -> MIN_SKIN // Go to first if after last
            else -> newSkin
        }
        
        currentSkin = finalSkin
        AppLogger.log("CustomizationFragment", "Updated currentSkin to: $currentSkin (circular navigation)")
        updateCatImage()
        updateSkinInfo()
        updateButtonStates()
        AppLogger.log("CustomizationFragment", "✓ Changed skin to $currentSkin")
    }
    
    private fun updateCatImage() {
        AppLogger.log("CustomizationFragment", "updateCatImage called: currentSkin=$currentSkin, PREVIEW_STAGE=$PREVIEW_STAGE")
        
        // Show stage 3a (PREVIEW_STAGE) of the current skin
        val drawableResId = when (currentSkin) {
            0 -> R.drawable.cat_0_stage_3
            1 -> R.drawable.cat_1_stage_3
            2 -> R.drawable.cat_2_stage_3
            3 -> R.drawable.cat_3_stage_3
            else -> {
                AppLogger.log("CustomizationFragment", "⚠ Unknown skin: $currentSkin, using default")
                R.drawable.cat_0_stage_3
            }
        }
        
        AppLogger.log("CustomizationFragment", "Setting drawable resource: $drawableResId")
        
        try {
            catImageView.setImageResource(drawableResId)
            AppLogger.log("CustomizationFragment", "✓ Image resource set successfully")
        } catch (e: Exception) {
            AppLogger.log("CustomizationFragment", "✗ Error setting image resource: ${e.message}")
        }
        
        // Update button states
        updateButtonStates()
    }
    
    private fun updateButtonStates() {
        // Both arrows are always enabled in circular carousel mode
        // But we can disable them if you want non-circular behavior
        // For now, keeping them always enabled for circular carousel
        prevSkinButton.isEnabled = true
        nextSkinButton.isEnabled = true
        
        AppLogger.log("CustomizationFragment", "Button states updated: currentSkin=$currentSkin (circular carousel)")
    }
    
    private fun updateSkinInfo() {
        val skinName = when (currentSkin) {
            0 -> "Gato Original"
            1 -> "Gato Variante 1"
            2 -> "Gato Variante 2"
            3 -> "Gato Variante 3"
            else -> "Gato Original"
        }
        skinNameText.text = skinName
    }
    
    private fun saveSelectedSkin(mainActivity: MainActivity?) {
        mainActivity?.let {
            // Check if user has completed their data
            val hasNickname = !it.userPreferences.getNickname().isNullOrEmpty()
            val hasAge = it.userPreferences.getAge() != null
            val hasGender = !it.userPreferences.getGender().isNullOrEmpty()
            val hasLocation = !it.userPreferences.getLocation().isNullOrEmpty()
            
            if (!hasNickname || !hasAge || !hasGender || !hasLocation) {
                // User hasn't completed data - show message and navigate to UserData
                android.widget.Toast.makeText(
                    context,
                    "Por favor completa tus datos primero",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                navigateToUserData()
                return
            }
            
            // Save skin and mark setup as complete
            it.userPreferences.setSelectedSkin(currentSkin)
            it.userPreferences.setInitialSetupComplete(true)
            AppLogger.log("CustomizationFragment", "✓ Saved selected skin: $currentSkin and marked setup as complete")
            
            // Disable buttons immediately
            prevSkinButton.isEnabled = false
            nextSkinButton.isEnabled = false
            selectButton.isEnabled = false
            selectButton.text = "Configuración completada"
            
            // Update tab state in MainActivity
            it.updatePersonalizationTabState()
            
            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "¡Configuración completada! Skin seleccionado: ${skinNameText.text}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Navigate to Gato tab to show the change immediately
            it.viewPager.currentItem = 0
            AppLogger.log("CustomizationFragment", "✓ Navigated to Gato tab to show selected skin")
        } ?: run {
            AppLogger.log("CustomizationFragment", "✗ Cannot save skin: MainActivity is null")
        }
    }
    
    private fun navigateToUserData() {
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            // Navigate to UserDataFragment tab (position 4)
            it.viewPager.currentItem = 4
            AppLogger.log("CustomizationFragment", "✓ Navigated to UserDataFragment")
        } ?: run {
            AppLogger.log("CustomizationFragment", "✗ Cannot navigate: MainActivity is null")
        }
    }
}

