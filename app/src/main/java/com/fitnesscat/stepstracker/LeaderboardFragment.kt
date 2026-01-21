package com.fitnesscat.stepstracker

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

class LeaderboardFragment : Fragment() {
    
    private lateinit var userStepsText: TextView
    private lateinit var amigo1StepsText: TextView
    private lateinit var amigo2StepsText: TextView
    private lateinit var userRankText: TextView
    private lateinit var amigo1RankText: TextView
    private lateinit var amigo2RankText: TextView
    
    private val mainHandler = Handler(Looper.getMainLooper())
    private var updateRunnable: Runnable? = null
    
    // Update intervals
    private val UPDATE_INTERVAL_MS = 2000L // Update every 2 seconds
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize views
        userStepsText = view.findViewById(R.id.userStepsText)
        amigo1StepsText = view.findViewById(R.id.amigo1StepsText)
        amigo2StepsText = view.findViewById(R.id.amigo2StepsText)
        userRankText = view.findViewById(R.id.userRankText)
        amigo1RankText = view.findViewById(R.id.amigo1RankText)
        amigo2RankText = view.findViewById(R.id.amigo2RankText)
        
        // Start periodic updates
        startPeriodicUpdates()
    }
    
    private fun startPeriodicUpdates() {
        updateRunnable = object : Runnable {
            override fun run() {
                updateLeaderboard()
                mainHandler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
        mainHandler.post(updateRunnable!!)
    }
    
    private fun updateLeaderboard() {
        val activity = activity as? MainActivity ?: return
        val userPreferences = activity.userPreferences
        
        // Get current user steps
        val userSteps = userPreferences.getTotalStepCount().toFloat()
        
        // Calculate friend steps (70% and 50% of user steps)
        val amigo1Steps = (userSteps * 0.7f).toInt()
        val amigo2Steps = (userSteps * 0.5f).toInt()
        
        // Create list of users with their steps for ranking
        val users = listOf(
            LeaderboardUser("Tú", userSteps.toInt(), true),
            LeaderboardUser("Amigo1", amigo1Steps, false),
            LeaderboardUser("Amigo2", amigo2Steps, false)
        ).sortedByDescending { it.steps }
        
        // Update UI based on ranking
        users.forEachIndexed { index, user ->
            val rank = index + 1
            when (user.name) {
                "Tú" -> {
                    userStepsText.text = "${user.steps} pasos"
                    userRankText.text = "#$rank"
                }
                "Amigo1" -> {
                    amigo1StepsText.text = "${user.steps} pasos"
                    amigo1RankText.text = "#$rank"
                }
                "Amigo2" -> {
                    amigo2StepsText.text = "${user.steps} pasos"
                    amigo2RankText.text = "#$rank"
                }
            }
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        updateRunnable?.let { mainHandler.removeCallbacks(it) }
    }
    
    private data class LeaderboardUser(
        val name: String,
        val steps: Int,
        val isCurrentUser: Boolean
    )
}

