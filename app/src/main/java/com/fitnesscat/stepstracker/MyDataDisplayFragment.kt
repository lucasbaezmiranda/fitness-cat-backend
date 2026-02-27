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
    private lateinit var countryText: TextView
    private lateinit var cityText: TextView
    private lateinit var urbanContextText: TextView

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
        countryText = view.findViewById(R.id.countryText)
        cityText = view.findViewById(R.id.cityText)
        urbanContextText = view.findViewById(R.id.urbanContextText)

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
        val country = mainActivity.userPreferences.getCountry()
        val city = mainActivity.userPreferences.getCity()
        val urbanContext = mainActivity.userPreferences.getUrbanContext()
        val catCode = mainActivity.userPreferences.getCatCode()

        // Display user data
        nicknameText.text = nickname ?: "Not specified"
        ageText.text = age?.toString() ?: "Not specified"
        genderText.text = gender ?: "Not specified"
        countryText.text = country ?: "Not specified"
        cityText.text = city ?: "Not specified"
        urbanContextText.text = urbanContext ?: "Not specified"

        // Display selected cat (showing stage 3 as preview)
        val drawableName = "cat_${catCode}_stage_3"
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        catImageView.setImageResource(if (resId != 0) resId else R.drawable.cat_stage_1)

        android.util.Log.d("MyDataDisplayFragment", "Loaded user data: nickname=$nickname, age=$age, gender=$gender, country=$country, city=$city, urbanContext=$urbanContext, catCode=$catCode")
    }
}
