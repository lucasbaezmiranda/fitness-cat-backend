package com.fitnesscat.stepstracker

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment

class UserDataFragment : Fragment() {
    
    private lateinit var nicknameEditText: EditText
    private lateinit var ageEditText: EditText
    private lateinit var genderSpinner: Spinner
    private lateinit var locationEditText: EditText
    private lateinit var saveButton: Button
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user_data, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        nicknameEditText = view.findViewById(R.id.nicknameEditText)
        ageEditText = view.findViewById(R.id.ageEditText)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        locationEditText = view.findViewById(R.id.locationEditText)
        saveButton = view.findViewById(R.id.saveDataButton)
        
        AppLogger.log("UserDataFragment", "Views initialized")
        
        // Get MainActivity to access shared objects
        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            // Load saved data
            loadUserData(mainActivity)
        } else {
            AppLogger.log("UserDataFragment", "⚠ MainActivity is null!")
        }
        
        // Set up save button listener
        saveButton.setOnClickListener {
            AppLogger.log("UserDataFragment", "▶ Save button clicked")
            saveUserData(mainActivity)
        }
        
        AppLogger.log("UserDataFragment", "✓ Fragment setup complete")
    }
    
    override fun onResume() {
        super.onResume()
        AppLogger.log("UserDataFragment", "onResume called")
        
        // Reload data in case it was changed elsewhere
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            loadUserData(it)
        }
    }
    
    private fun loadUserData(mainActivity: MainActivity) {
        val nickname = mainActivity.userPreferences.getNickname()
        val age = mainActivity.userPreferences.getAge()
        val gender = mainActivity.userPreferences.getGender()
        val location = mainActivity.userPreferences.getLocation()
        
        nicknameEditText.setText(nickname ?: "")
        ageEditText.setText(age?.toString() ?: "")
        locationEditText.setText(location ?: "")
        
        // Set gender spinner selection
        val genderArray = resources.getStringArray(R.array.gender_options)
        val genderIndex = genderArray.indexOfFirst { it.equals(gender, ignoreCase = true) }
        if (genderIndex >= 0) {
            genderSpinner.setSelection(genderIndex)
        }
        
        AppLogger.log("UserDataFragment", "Loaded user data: nickname=$nickname, age=$age, gender=$gender, location=$location")
    }
    
    private fun saveUserData(mainActivity: MainActivity?) {
        mainActivity?.let {
            val nickname = nicknameEditText.text.toString().trim()
            val ageStr = ageEditText.text.toString().trim()
            val age = ageStr.toIntOrNull()
            val gender = genderSpinner.selectedItem?.toString() ?: ""
            val location = locationEditText.text.toString().trim()
            
            it.userPreferences.setNickname(if (nickname.isNotEmpty()) nickname else null)
            it.userPreferences.setAge(age)
            it.userPreferences.setGender(if (gender.isNotEmpty()) gender else null)
            it.userPreferences.setLocation(if (location.isNotEmpty()) location else null)
            
            AppLogger.log("UserDataFragment", "✓ Saved user data: nickname=$nickname, age=$age, gender=$gender, location=$location")
            
            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "Datos guardados correctamente",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        } ?: run {
            AppLogger.log("UserDataFragment", "✗ Cannot save data: MainActivity is null")
        }
    }
}

