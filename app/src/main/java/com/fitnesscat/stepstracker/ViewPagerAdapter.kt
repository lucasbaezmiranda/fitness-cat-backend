package com.fitnesscat.stepstracker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    private val userPreferences: UserPreferences = UserPreferences(fragmentActivity)
    
    override fun getItemCount(): Int = 5
    
    override fun getItemId(position: Int): Long {
        // Use different IDs for position 2 depending on setup state
        // This forces recreation of the fragment when setup state changes
        return if (position == 2) {
            if (userPreferences.isInitialSetupComplete()) {
                200L // MyDataDisplayFragment
            } else {
                201L // CustomizationFragment
            }
        } else {
            super.getItemId(position)
        }
    }
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserFragment()
            1 -> LeaderboardFragment()
            2 -> {
                // Show CustomizationFragment if setup not complete, otherwise MyDataDisplayFragment
                if (userPreferences.isInitialSetupComplete()) {
                    MyDataDisplayFragment()
                } else {
                    CustomizationFragment()
                }
            }
            3 -> DevFragment()
            4 -> UserDataFragment()
            else -> UserFragment()
        }
    }
}






