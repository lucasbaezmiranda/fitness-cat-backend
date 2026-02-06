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
        
        android.util.Log.d("CustomizationFragment", "Views initialized")
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved skin
            currentSkin = mainActivity.userPreferences.getSelectedSkin()
            android.util.Log.d("CustomizationFragment", "Loaded skin from preferences: $currentSkin")
        } else {
            android.util.Log.w("CustomizationFragment", "MainActivity is null!")
        }
        
        // Set up button listeners
        prevSkinButton.setOnClickListener {
            android.util.Log.d("CustomizationFragment", "Prev button clicked, currentSkin: $currentSkin")
            changeSkin(-1)
        }
        
        nextSkinButton.setOnClickListener {
            android.util.Log.d("CustomizationFragment", "Next button clicked, currentSkin: $currentSkin")
            changeSkin(1)
        }
        
        selectButton.setOnClickListener {
            android.util.Log.d("CustomizationFragment", "Select button clicked")
            saveSelectedSkin(mainActivity)
        }
        
        // Update UI with loaded values
        updateCatImage()
        updateSkinInfo()
        
        android.util.Log.d("CustomizationFragment", "Fragment setup complete")
    }
    
    override fun onResume() {
        super.onResume()
        android.util.Log.d("CustomizationFragment", "onResume called")
        
        // Reload skin in case it was changed elsewhere
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            val savedSkin = it.userPreferences.getSelectedSkin()
            if (savedSkin != currentSkin) {
                android.util.Log.d("CustomizationFragment", "Skin changed externally: $currentSkin -> $savedSkin")
                currentSkin = savedSkin
                updateCatImage()
                updateSkinInfo()
            }
        }
    }
    
    private fun changeSkin(delta: Int) {
        val newSkin = currentSkin + delta
        
        android.util.Log.d("CustomizationFragment", "changeSkin called: delta=$delta, currentSkin=$currentSkin, newSkin=$newSkin")
        
        if (newSkin < MIN_SKIN || newSkin > MAX_SKIN) {
            android.util.Log.w("CustomizationFragment", "Skin out of bounds: $newSkin (min=$MIN_SKIN, max=$MAX_SKIN)")
            return
        }
        
        currentSkin = newSkin
        android.util.Log.d("CustomizationFragment", "Updated currentSkin to: $currentSkin")
        updateCatImage()
        updateSkinInfo()
        android.util.Log.d("CustomizationFragment", "Changed skin to $currentSkin")
    }
    
    private fun updateCatImage() {
        android.util.Log.d("CustomizationFragment", "updateCatImage called: currentSkin=$currentSkin, PREVIEW_STAGE=$PREVIEW_STAGE")
        
        // Show stage 3a (PREVIEW_STAGE) of the current skin
        val drawableResId = when (currentSkin) {
            0 -> R.drawable.cat_0_stage_3
            1 -> R.drawable.cat_1_stage_3
            2 -> R.drawable.cat_2_stage_3
            3 -> R.drawable.cat_3_stage_3
            else -> {
                android.util.Log.w("CustomizationFragment", "Unknown skin: $currentSkin, using default")
                R.drawable.cat_0_stage_3
            }
        }
        
        android.util.Log.d("CustomizationFragment", "Setting drawable resource: $drawableResId")
        
        try {
            catImageView.setImageResource(drawableResId)
            android.util.Log.d("CustomizationFragment", "Image resource set successfully")
        } catch (e: Exception) {
            android.util.Log.e("CustomizationFragment", "Error setting image resource: ${e.message}", e)
        }
        
        // Update button states
        val prevEnabled = currentSkin > MIN_SKIN
        val nextEnabled = currentSkin < MAX_SKIN
        prevSkinButton.isEnabled = prevEnabled
        nextSkinButton.isEnabled = nextEnabled
        
        android.util.Log.d("CustomizationFragment", "Button states: prevEnabled=$prevEnabled, nextEnabled=$nextEnabled")
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
            android.util.Log.d("CustomizationFragment", "Saved selected skin: $currentSkin")
            
            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "Skin seleccionado: ${skinNameText.text}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            
            // Notify UserFragment to update if it's visible
            // The UserFragment will update on next resume
        }
    }
}

