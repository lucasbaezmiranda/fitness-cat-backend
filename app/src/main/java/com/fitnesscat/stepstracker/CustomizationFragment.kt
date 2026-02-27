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
    private lateinit var arrowPiel: Button
    private lateinit var arrowManchas: Button
    private lateinit var arrowOjos: Button
    private lateinit var arrowOrejas: Button
    private lateinit var valuePiel: TextView
    private lateinit var valueManchas: TextView
    private lateinit var valueOjos: TextView
    private lateinit var valueOrejas: TextView
    private lateinit var selectButton: Button
    private lateinit var myDataButton: Button

    private var piel    = 0
    private var manchas = 0
    private var ojos    = 0
    private var orejas  = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_customization, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        catImageView  = view.findViewById(R.id.catImageView)
        arrowPiel     = view.findViewById(R.id.arrowPiel)
        arrowManchas  = view.findViewById(R.id.arrowManchas)
        arrowOjos     = view.findViewById(R.id.arrowOjos)
        arrowOrejas   = view.findViewById(R.id.arrowOrejas)
        valuePiel     = view.findViewById(R.id.valuePiel)
        valueManchas  = view.findViewById(R.id.valueManchas)
        valueOjos     = view.findViewById(R.id.valueOjos)
        valueOrejas   = view.findViewById(R.id.valueOrejas)
        selectButton  = view.findViewById(R.id.selectButton)
        myDataButton  = view.findViewById(R.id.myDataButton)

        val mainActivity = activity as? MainActivity
        if (mainActivity != null) {
            piel    = mainActivity.userPreferences.getCatPiel()
            manchas = mainActivity.userPreferences.getCatManchas()
            ojos    = mainActivity.userPreferences.getCatOjos()
            orejas  = mainActivity.userPreferences.getCatOrejas()

            if (mainActivity.userPreferences.isInitialSetupComplete()) {
                setArrowsEnabled(false)
                selectButton.isEnabled = false
                selectButton.text = "Setup complete"
            }
        }

        arrowPiel.setOnClickListener    { piel    = 1 - piel;    updateUI() }
        arrowManchas.setOnClickListener { manchas = 1 - manchas; updateUI() }
        arrowOjos.setOnClickListener    { ojos    = 1 - ojos;    updateUI() }
        arrowOrejas.setOnClickListener  { orejas  = 1 - orejas;  updateUI() }

        selectButton.setOnClickListener { saveSelectedSkin(mainActivity) }
        myDataButton.setOnClickListener { navigateToUserData() }

        updateUI()
    }

    override fun onResume() {
        super.onResume()
        val mainActivity = activity as? MainActivity
        mainActivity?.let {
            piel    = it.userPreferences.getCatPiel()
            manchas = it.userPreferences.getCatManchas()
            ojos    = it.userPreferences.getCatOjos()
            orejas  = it.userPreferences.getCatOrejas()
            updateUI()
        }
    }

    private fun updateUI() {
        valuePiel.text    = piel.toString()
        valueManchas.text = manchas.toString()
        valueOjos.text    = ojos.toString()
        valueOrejas.text  = orejas.toString()

        val code = "$piel$manchas$ojos$orejas"
        val drawableName = "cat_${code}_stage_3"
        val resId = resources.getIdentifier(drawableName, "drawable", requireContext().packageName)
        catImageView.setImageResource(if (resId != 0) resId else R.drawable.cat_stage_1)
    }

    private fun setArrowsEnabled(enabled: Boolean) {
        arrowPiel.isEnabled    = enabled
        arrowManchas.isEnabled = enabled
        arrowOjos.isEnabled    = enabled
        arrowOrejas.isEnabled  = enabled
    }

    private fun saveSelectedSkin(mainActivity: MainActivity?) {
        mainActivity?.let {
            val hasNickname = !it.userPreferences.getNickname().isNullOrEmpty()
            val hasAge      = !it.userPreferences.getAge().isNullOrEmpty()
            val hasGender   = !it.userPreferences.getGender().isNullOrEmpty()
            val hasCountry  = !it.userPreferences.getCountry().isNullOrEmpty()

            if (!hasNickname || !hasAge || !hasGender || !hasCountry) {
                android.widget.Toast.makeText(
                    context,
                    "Please complete your data first",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                navigateToUserData()
                return
            }

            it.userPreferences.setCatCharacteristics(piel, manchas, ojos, orejas)
            it.userPreferences.setInitialSetupComplete(true)

            setArrowsEnabled(false)
            selectButton.isEnabled = false
            selectButton.text = "Setup complete"

            it.updatePersonalizationTabState()

            android.widget.Toast.makeText(
                context,
                "Setup complete!",
                android.widget.Toast.LENGTH_SHORT
            ).show()

            it.updatePersonalizationTabState()
            it.viewPager.currentItem = 0
        }
    }

    private fun navigateToUserData() {
        val mainActivity = activity as? MainActivity
        mainActivity?.viewPager?.currentItem = 4
    }
}
