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
    private lateinit var estilo1Button: Button
    private lateinit var estilo2Button: Button
    private lateinit var estilo3Button: Button
    private lateinit var estilo4Button: Button
    private lateinit var skinNameText: TextView
    private lateinit var selectButton: Button
    
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
        estilo1Button = view.findViewById(R.id.estilo1Button)
        estilo2Button = view.findViewById(R.id.estilo2Button)
        estilo3Button = view.findViewById(R.id.estilo3Button)
        estilo4Button = view.findViewById(R.id.estilo4Button)
        skinNameText = view.findViewById(R.id.skinNameText)
        selectButton = view.findViewById(R.id.selectButton)
        
        AppLogger.log("CustomizationFragment", "Views initialized")
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved skin
            currentSkin = mainActivity.userPreferences.getSelectedSkin()
            AppLogger.log("CustomizationFragment", "Loaded skin from preferences: $currentSkin")
        } else {
            AppLogger.log("CustomizationFragment", "⚠ MainActivity is null!")
        }
        
        // Set up button listeners
        estilo1Button.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Estilo 1 button clicked")
            selectSkin(0)
        }
        
        estilo2Button.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Estilo 2 button clicked")
            selectSkin(1)
        }
        
        estilo3Button.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Estilo 3 button clicked")
            selectSkin(2)
        }
        
        estilo4Button.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Estilo 4 button clicked")
            selectSkin(3)
        }
        
        selectButton.setOnClickListener {
            AppLogger.log("CustomizationFragment", "▶ Select button clicked")
            saveSelectedSkin(mainActivity)
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
    
    private fun selectSkin(skin: Int) {
        AppLogger.log("CustomizationFragment", "selectSkin called: skin=$skin")
        
        if (skin < MIN_SKIN || skin > MAX_SKIN) {
            AppLogger.log("CustomizationFragment", "✗ Skin out of bounds: $skin (min=$MIN_SKIN, max=$MAX_SKIN)")
            return
        }
        
        currentSkin = skin
        AppLogger.log("CustomizationFragment", "Updated currentSkin to: $currentSkin")
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
        // Highlight the currently selected button
        estilo1Button.isSelected = (currentSkin == 0)
        estilo2Button.isSelected = (currentSkin == 1)
        estilo3Button.isSelected = (currentSkin == 2)
        estilo4Button.isSelected = (currentSkin == 3)
        
        AppLogger.log("CustomizationFragment", "Button states updated: currentSkin=$currentSkin")
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
            it.userPreferences.setSelectedSkin(currentSkin)
            AppLogger.log("CustomizationFragment", "✓ Saved selected skin: $currentSkin")
            
            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "Skin seleccionado: ${skinNameText.text}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Notify UserFragment to update if it's visible
            // The UserFragment will update on next resume
        } ?: run {
            AppLogger.log("CustomizationFragment", "✗ Cannot save skin: MainActivity is null")
        }
    }
}

