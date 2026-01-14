package com.fitnesscat.stepstracker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.ActivityRecognitionResult
import com.google.android.gms.location.DetectedActivity

/**
 * BroadcastReceiver that receives activity recognition updates from Google Play Services
 */
class ActivityRecognitionReceiver : BroadcastReceiver() {
    
    override fun onReceive(context: Context, intent: Intent) {
        if (ActivityRecognitionResult.hasResult(intent)) {
            val result = ActivityRecognitionResult.extractResult(intent)
            val mostProbableActivity = result.mostProbableActivity
            
            val activityType = mostProbableActivity.type
            val confidence = mostProbableActivity.confidence
            
            // Get activity name
            val activityName = getActivityName(activityType)
            
            Log.d("ActivityRecognition", "Detected activity: $activityName (confidence: $confidence%)")
            AppLogger.log("ActivityRecognition", "Activity: $activityName ($confidence%)")
            
            // Save to UserPreferences
            val userPreferences = UserPreferences(context)
            userPreferences.setCurrentActivity(activityType, activityName)
            
            // Notify MainActivity if it's available
            val mainActivity = MainActivity.instance
            mainActivity?.onActivityDetected(activityName, confidence)
        }
    }
    
    /**
     * Converts activity type constant to human-readable name
     */
    private fun getActivityName(activityType: Int): String {
        return when (activityType) {
            DetectedActivity.IN_VEHICLE -> "En vehículo"
            DetectedActivity.ON_BICYCLE -> "En bicicleta"
            DetectedActivity.ON_FOOT -> "A pie"
            DetectedActivity.RUNNING -> "Corriendo"
            DetectedActivity.STILL -> "Quieto"
            DetectedActivity.TILTING -> "Inclinando"
            DetectedActivity.WALKING -> "Caminando"
            DetectedActivity.UNKNOWN -> "Desconocido"
            else -> "Desconocido"
        }
    }
}

