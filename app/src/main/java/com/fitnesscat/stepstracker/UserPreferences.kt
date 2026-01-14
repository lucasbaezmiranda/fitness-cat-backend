package com.fitnesscat.stepstracker

import android.content.Context
import android.content.SharedPreferences

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
        private const val KEY_CURRENT_STAGE = "current_stage"  // Current stage (1-3)
        private const val KEY_CURRENT_HEALTH = "current_health"  // Current health (0-100)
        private const val KEY_CURRENT_ACTIVITY_TYPE = "current_activity_type"  // Activity type constant
        private const val KEY_CURRENT_ACTIVITY_NAME = "current_activity_name"  // Activity name (human-readable)
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
     * Adds a new step record to pending records (as JSON string)
     * @param stepsAtTime Steps count at this timestamp
     * @param timestamp Unix timestamp in seconds
     */
    fun addPendingStepRecord(stepsAtTime: Int, timestamp: Long) {
        try {
            val pendingJson = getPendingStepRecords()
            
            // Construir nuevo record como string JSON
            val newRecord = "{\"steps_at_time\":$stepsAtTime,\"timestamp\":$timestamp}"
            
            // Si está vacío, crear array nuevo, sino agregar al existente
            val updatedJson = if (pendingJson == "[]") {
                "[$newRecord]"
            } else {
                // Remover el "]" final, agregar coma y nuevo record, luego cerrar array
                pendingJson.dropLast(1) + ",$newRecord]"
            }
            
            savePendingStepRecords(updatedJson)
            android.util.Log.d("UserPreferences", "Added pending record: steps=$stepsAtTime, timestamp=$timestamp")
            android.util.Log.d("UserPreferences", "Updated JSON: $updatedJson")
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
    
    /**
     * Gets current stage (1-5), default is 1
     */
    fun getCurrentStage(): Int {
        return prefs.getInt(KEY_CURRENT_STAGE, 1)
    }
    
    /**
     * Saves current stage (1-5)
     */
    fun setCurrentStage(stage: Int) {
        prefs.edit().putInt(KEY_CURRENT_STAGE, stage).apply()
        android.util.Log.d("UserPreferences", "Saved current stage: $stage")
    }
    
    /**
     * Gets current health (0-100), default is 100
     */
    fun getCurrentHealth(): Int {
        return prefs.getInt(KEY_CURRENT_HEALTH, 100)
    }
    
    /**
     * Saves current health (0-100)
     */
    fun setCurrentHealth(health: Int) {
        prefs.edit().putInt(KEY_CURRENT_HEALTH, health).apply()
        android.util.Log.d("UserPreferences", "Saved current health: $health")
    }
    
    /**
     * Gets current activity type (constant from DetectedActivity)
     */
    fun getCurrentActivityType(): Int {
        return prefs.getInt(KEY_CURRENT_ACTIVITY_TYPE, com.google.android.gms.location.DetectedActivity.UNKNOWN)
    }
    
    /**
     * Gets current activity name (human-readable)
     */
    fun getCurrentActivityName(): String {
        return prefs.getString(KEY_CURRENT_ACTIVITY_NAME, "Desconocido") ?: "Desconocido"
    }
    
    /**
     * Saves current activity (type and name)
     */
    fun setCurrentActivity(activityType: Int, activityName: String) {
        prefs.edit()
            .putInt(KEY_CURRENT_ACTIVITY_TYPE, activityType)
            .putString(KEY_CURRENT_ACTIVITY_NAME, activityName)
            .apply()
        android.util.Log.d("UserPreferences", "Saved current activity: $activityName (type: $activityType)")
    }
}

