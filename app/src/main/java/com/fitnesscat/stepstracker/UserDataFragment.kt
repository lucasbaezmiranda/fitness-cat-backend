package com.fitnesscat.stepstracker

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment

class UserDataFragment : Fragment() {

    private lateinit var nicknameEditText: EditText
    private lateinit var ageSpinner: Spinner
    private lateinit var genderSpinner: Spinner
    private lateinit var countrySpinner: Spinner
    private lateinit var cityEditText: EditText
    private lateinit var urbanContextSpinner: Spinner
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
        ageSpinner = view.findViewById(R.id.ageSpinner)
        genderSpinner = view.findViewById(R.id.genderSpinner)
        countrySpinner = view.findViewById(R.id.countrySpinner)
        cityEditText = view.findViewById(R.id.cityEditText)
        urbanContextSpinner = view.findViewById(R.id.urbanContextSpinner)
        saveButton = view.findViewById(R.id.saveDataButton)

        // Set spinner adapters with black text for light backgrounds
        setupSpinner(ageSpinner, R.array.age_options)
        setupSpinner(genderSpinner, R.array.gender_options)
        setupSpinner(countrySpinner, R.array.country_array)
        setupSpinner(urbanContextSpinner, R.array.urban_context_options)

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
        val country = mainActivity.userPreferences.getCountry()
        val city = mainActivity.userPreferences.getCity()
        val urbanContext = mainActivity.userPreferences.getUrbanContext()

        nicknameEditText.setText(nickname ?: "")
        cityEditText.setText(city ?: "")

        // Set age spinner selection
        val ageArray = resources.getStringArray(R.array.age_options)
        val ageIndex = ageArray.indexOfFirst { it.equals(age, ignoreCase = true) }
        if (ageIndex >= 0) {
            ageSpinner.setSelection(ageIndex)
        }

        // Set gender spinner selection
        val genderArray = resources.getStringArray(R.array.gender_options)
        val genderIndex = genderArray.indexOfFirst { it.equals(gender, ignoreCase = true) }
        if (genderIndex >= 0) {
            genderSpinner.setSelection(genderIndex)
        }

        // Set country spinner selection
        val countryArray = resources.getStringArray(R.array.country_array)
        val countryIndex = countryArray.indexOfFirst { it.equals(country, ignoreCase = true) }
        if (countryIndex >= 0) {
            countrySpinner.setSelection(countryIndex)
        }

        // Set urban context spinner selection
        val urbanContextArray = resources.getStringArray(R.array.urban_context_options)
        val urbanContextIndex = urbanContextArray.indexOfFirst { it.equals(urbanContext, ignoreCase = true) }
        if (urbanContextIndex >= 0) {
            urbanContextSpinner.setSelection(urbanContextIndex)
        }

        AppLogger.log("UserDataFragment", "Loaded user data: nickname=$nickname, age=$age, gender=$gender, country=$country, city=$city, urbanContext=$urbanContext")
    }

    private fun setupSpinner(spinner: Spinner, arrayResId: Int) {
        val items = resources.getStringArray(arrayResId)
        val adapter = object : ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_spinner_item,
            items.toList()
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getView(position, convertView, parent) as TextView
                view.setTextColor(Color.BLACK)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = super.getDropDownView(position, convertView, parent) as TextView
                view.setTextColor(Color.BLACK)
                view.setBackgroundColor(Color.WHITE)
                return view
            }
        }
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun saveUserData(mainActivity: MainActivity?) {
        mainActivity?.let {
            val nickname = nicknameEditText.text.toString().trim()
            val age = ageSpinner.selectedItem?.toString() ?: ""
            val gender = genderSpinner.selectedItem?.toString() ?: ""
            val country = countrySpinner.selectedItem?.toString() ?: ""
            val city = cityEditText.text.toString().trim()
            val urbanContext = urbanContextSpinner.selectedItem?.toString() ?: ""

            it.userPreferences.setNickname(if (nickname.isNotEmpty()) nickname else null)
            it.userPreferences.setAge(if (age.isNotEmpty()) age else null)
            it.userPreferences.setGender(if (gender.isNotEmpty()) gender else null)
            it.userPreferences.setCountry(if (country.isNotEmpty()) country else null)
            it.userPreferences.setCity(if (city.isNotEmpty()) city else null)
            it.userPreferences.setUrbanContext(if (urbanContext.isNotEmpty()) urbanContext else null)

            AppLogger.log("UserDataFragment", "✓ Saved user data: nickname=$nickname, age=$age, gender=$gender, country=$country, city=$city, urbanContext=$urbanContext")

            // Sync user profile to backend
            it.apiClient.syncUserProfile(
                userId = it.userPreferences.getUserId(),
                nickname = if (nickname.isNotEmpty()) nickname else null,
                age = if (age.isNotEmpty()) age else null,
                gender = if (gender.isNotEmpty()) gender else null,
                country = if (country.isNotEmpty()) country else null,
                city = if (city.isNotEmpty()) city else null,
                urbanContext = if (urbanContext.isNotEmpty()) urbanContext else null,
                fcmToken = it.userPreferences.getFcmToken()
            )

            // Show confirmation
            android.widget.Toast.makeText(
                context,
                "Data saved",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            // If initial setup is not complete, navigate to Customization tab
            if (!it.userPreferences.isInitialSetupComplete()) {
                it.viewPager.currentItem = 2  // Customization tab
            }
        } ?: run {
            AppLogger.log("UserDataFragment", "✗ Cannot save data: MainActivity is null")
        }
    }
}
