package com.fitnesscat.stepstracker

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    
    override fun getItemCount(): Int = 5
    
    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> UserFragment()
            1 -> LeaderboardFragment()
            2 -> CustomizationFragment()
            3 -> DevFragment()
            4 -> UserDataFragment()
            else -> UserFragment()
        }
    }
}






