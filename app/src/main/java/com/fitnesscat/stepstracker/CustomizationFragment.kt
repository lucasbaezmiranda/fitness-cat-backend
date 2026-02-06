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
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved skin
            currentSkin = mainActivity.userPreferences.getSelectedSkin()
        }
        
        // Set up button listeners
        prevSkinButton.setOnClickListener {
            changeSkin(-1)
        }
        
        nextSkinButton.setOnClickListener {
            changeSkin(1)
        }
        
        selectButton.setOnClickListener {
            saveSelectedSkin(mainActivity)
        }
        
        // Update UI with loaded values
        updateCatImage()
        updateSkinInfo()
    }
    
    private fun changeSkin(delta: Int) {
        val newSkin = currentSkin + delta
        
        if (newSkin < MIN_SKIN || newSkin > MAX_SKIN) {
            return
        }
        
        currentSkin = newSkin
        updateCatImage()
        updateSkinInfo()
        android.util.Log.d("CustomizationFragment", "Changed skin to $currentSkin")
    }
    
    private fun updateCatImage() {
        // Show stage 3a (PREVIEW_STAGE) of the current skin
        val drawableResId = when {
            currentSkin == 0 -> when (PREVIEW_STAGE) {
                1 -> R.drawable.cat_0_stage_1
                2 -> R.drawable.cat_0_stage_2
                3 -> R.drawable.cat_0_stage_3
                4 -> R.drawable.cat_0_stage_4
                5 -> R.drawable.cat_0_stage_5
                else -> R.drawable.cat_0_stage_3
            }
            currentSkin == 1 -> when (PREVIEW_STAGE) {
                1 -> R.drawable.cat_1_stage_1
                2 -> R.drawable.cat_1_stage_2
                3 -> R.drawable.cat_1_stage_3
                4 -> R.drawable.cat_1_stage_4
                5 -> R.drawable.cat_1_stage_5
                else -> R.drawable.cat_1_stage_3
            }
            currentSkin == 2 -> when (PREVIEW_STAGE) {
                1 -> R.drawable.cat_2_stage_1
                2 -> R.drawable.cat_2_stage_2
                3 -> R.drawable.cat_2_stage_3
                4 -> R.drawable.cat_2_stage_4
                5 -> R.drawable.cat_2_stage_5
                else -> R.drawable.cat_2_stage_3
            }
            currentSkin == 3 -> when (PREVIEW_STAGE) {
                1 -> R.drawable.cat_3_stage_1
                2 -> R.drawable.cat_3_stage_2
                3 -> R.drawable.cat_3_stage_3
                4 -> R.drawable.cat_3_stage_4
                5 -> R.drawable.cat_3_stage_5
                else -> R.drawable.cat_3_stage_3
            }
            else -> R.drawable.cat_0_stage_3
        }
        
        catImageView.setImageResource(drawableResId)
        prevSkinButton.isEnabled = currentSkin > MIN_SKIN
        nextSkinButton.isEnabled = currentSkin < MAX_SKIN
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

