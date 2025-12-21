package com.fitnesscat.stepstracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper class to manage user preferences, including user ID
 */
class UserPreferences(context: Context) {
    private val context: Context = context.applicationContext
    private val prefs: SharedPreferences = this.context.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )

    companion object {
        private const val PREFS_NAME = "StepTrackerPrefs"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_LAST_SYNC = "last_sync_timestamp"
        private const val KEY_TOTAL_STEP_COUNT = "total_step_count"  // Total steps accumulated
        private const val KEY_LAST_SENSOR_VALUE = "last_sensor_value"  // Last sensor reading
        private const val KEY_PENDING_STEP_RECORDS = "pending_step_records"  // JSON array of pending records
    }

    fun getUserId(): String {
        val userId = prefs.getString(KEY_USER_ID, null)
        return if (userId != null) {
            userId
        } else {
            // Generate a new unique user ID (using Android ID + timestamp)
            val newUserId = generateUserId()
            prefs.edit().putString(KEY_USER_ID, newUserId).apply()
            newUserId
        }
    }

    private fun generateUserId(): String {
        val androidId = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ANDROID_ID
        ) ?: "unknown"
        val timestamp = System.currentTimeMillis()
        return "${androidId}_$timestamp"
    }

    fun getLastSyncTimestamp(): Long {
        return prefs.getLong(KEY_LAST_SYNC, 0)
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC, timestamp).apply()
    }

    fun getTotalStepCount(): Int {
        return prefs.getInt(KEY_TOTAL_STEP_COUNT, 0)
    }

    fun setTotalStepCount(count: Int) {
        prefs.edit().putInt(KEY_TOTAL_STEP_COUNT, count).apply()
    }

    fun getLastSensorValue(): Float {
        return prefs.getFloat(KEY_LAST_SENSOR_VALUE, 0f)
    }

    fun setLastSensorValue(value: Float) {
        prefs.edit().putFloat(KEY_LAST_SENSOR_VALUE, value).apply()
    }
    
    /**
     * Gets pending step records as JSON array string
     * Format: [{"steps_at_time": 100, "timestamp": 1704123456}, ...]
     */
    fun getPendingStepRecords(): String {
        return prefs.getString(KEY_PENDING_STEP_RECORDS, "[]") ?: "[]"
    }
    
    /**
     * Saves pending step records as JSON array string
     * Format: [{"steps_at_time": 100, "timestamp": 1704123456}, ...]
     */
    fun savePendingStepRecords(jsonString: String) {
        prefs.edit().putString(KEY_PENDING_STEP_RECORDS, jsonString).apply()
    }
    
    /**
     * Adds a new step record to pending records
     * @param stepsAtTime Steps count at this timestamp
     * @param timestamp Unix timestamp in seconds
     */
    fun addPendingStepRecord(stepsAtTime: Int, timestamp: Long) {
        try {
            val pendingJson = getPendingStepRecords()
            val records = JSONArray(pendingJson)
            
            // Create new record
            val newRecord = JSONObject().apply {
                put("steps_at_time", stepsAtTime)
                put("timestamp", timestamp)
            }
            
            // Add to array
            records.put(newRecord)
            
            // Save back
            savePendingStepRecords(records.toString())
            android.util.Log.d("UserPreferences", "Added pending record: steps=$stepsAtTime, timestamp=$timestamp")
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error adding pending record: ${e.message}", e)
        }
    }
    
    /**
     * Clears all pending step records
     */
    fun clearPendingStepRecords() {
        prefs.edit().putString(KEY_PENDING_STEP_RECORDS, "[]").apply()
        android.util.Log.d("UserPreferences", "Cleared all pending step records")
    }
}

