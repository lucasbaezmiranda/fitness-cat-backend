package com.fitnesscat.stepstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Fragment to display user data and selected cat (read-only)
 * Replaces CustomizationFragment after initial setup is complete
 */
class MyDataDisplayFragment : Fragment() {
    
    private lateinit var catImageView: ImageView
    private lateinit var nicknameText: TextView
    private lateinit var ageText: TextView
    private lateinit var genderText: TextView
    private lateinit var locationText: TextView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_my_data_display, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        catImageView = view.findViewById(R.id.catImageView)
        nicknameText = view.findViewById(R.id.nicknameText)
        ageText = view.findViewById(R.id.ageText)
        genderText = view.findViewById(R.id.genderText)
        locationText = view.findViewById(R.id.locationText)
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            loadUserData(mainActivity)
        }
    }
    
    override fun onResume() {
        super.onResume()
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            loadUserData(it)
        }
    }
    
    private fun loadUserData(mainActivity: MainActivity) {
        // Load user data
        val nickname = mainActivity.userPreferences.getNickname()
        val age = mainActivity.userPreferences.getAge()
        val gender = mainActivity.userPreferences.getGender()
        val location = mainActivity.userPreferences.getLocation()
        val selectedSkin = mainActivity.userPreferences.getSelectedSkin()
        
        // Display user data
        nicknameText.text = nickname ?: "No especificado"
        ageText.text = age?.toString() ?: "No especificado"
        genderText.text = gender ?: "No especificado"
        locationText.text = location ?: "No especificado"
        
        // Display selected cat (showing stage 3 as preview)
        val drawableResId = when (selectedSkin) {
            0 -> R.drawable.cat_0_stage_3
            1 -> R.drawable.cat_1_stage_3
            2 -> R.drawable.cat_2_stage_3
            3 -> R.drawable.cat_3_stage_3
            else -> R.drawable.cat_0_stage_3
        }
        catImageView.setImageResource(drawableResId)
        
        android.util.Log.d("MyDataDisplayFragment", "Loaded user data: nickname=$nickname, age=$age, gender=$gender, location=$location, skin=$selectedSkin")
    }
}

