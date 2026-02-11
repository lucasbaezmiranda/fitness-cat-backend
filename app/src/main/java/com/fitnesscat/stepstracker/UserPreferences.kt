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
        private const val KEY_CURRENT_STAGE = "current_stage"  // Current stage (1-5)
        private const val KEY_CURRENT_HEALTH = "current_health"  // Current health (0-100)
        private const val KEY_CURRENT_ACTIVITY_TYPE = "current_activity_type"  // Activity type constant
        private const val KEY_CURRENT_ACTIVITY_NAME = "current_activity_name"  // Activity name (human-readable)
        private const val KEY_SELECTED_SKIN = "selected_skin"  // Selected cat skin (0-3)
        private const val KEY_NICKNAME = "nickname"  // User nickname
        private const val KEY_AGE = "age"  // User age
        private const val KEY_GENDER = "gender"  // User gender
        private const val KEY_LOCATION = "location"  // User location
        private const val KEY_INITIAL_SETUP_COMPLETE = "initial_setup_complete"  // Whether initial setup is done
        private const val KEY_DAILY_STEP_COUNT = "daily_step_count"  // Steps for current day
        private const val KEY_DAILY_STEP_DATE = "daily_step_date"  // Date of last daily step reset (YYYY-MM-DD)
        private const val KEY_TOTAL_STEPS_AT_DAY_START = "total_steps_at_day_start"  // Total steps at start of current day
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
     * Format: [{"steps": 100, "timestamp": 1704123456, "latitude": 40.7128, "longitude": -74.0060}, ...]
     */
    fun getPendingStepRecords(): String {
        return prefs.getString(KEY_PENDING_STEP_RECORDS, "[]") ?: "[]"
    }
    
    /**
     * Saves pending step records as JSON array string
     * Format: [{"steps": 100, "timestamp": 1704123456, "latitude": 40.7128, "longitude": -74.0060}, ...]
     */
    fun savePendingStepRecords(jsonString: String) {
        prefs.edit().putString(KEY_PENDING_STEP_RECORDS, jsonString).apply()
    }
    
    /**
     * Adds a new step record to pending records (as JSON string)
     * @param stepsAtTime Steps count at this timestamp
     * @param timestamp Unix timestamp in seconds
     * @param latitude GPS latitude (optional, can be null)
     * @param longitude GPS longitude (optional, can be null)
     */
    fun addPendingStepRecord(stepsAtTime: Int, timestamp: Long, latitude: Double? = null, longitude: Double? = null) {
        try {
            val pendingJson = getPendingStepRecords()
            
            // Construir nuevo record como string JSON (mismo formato que syncSteps individual)
            val coordinateStr = buildString {
                if (latitude != null) {
                    append(",\"latitude\":$latitude")
                }
                if (longitude != null) {
                    append(",\"longitude\":$longitude")
                }
            }
            
            val newRecord = "{\"steps\":$stepsAtTime,\"timestamp\":$timestamp$coordinateStr}"
            
            // Si está vacío, crear array nuevo, sino agregar al existente
            val updatedJson = if (pendingJson == "[]") {
                "[$newRecord]"
            } else {
                // Remover el "]" final, agregar coma y nuevo record, luego cerrar array
                pendingJson.dropLast(1) + ",$newRecord]"
            }
            
            savePendingStepRecords(updatedJson)
            android.util.Log.d("UserPreferences", "Added pending record: steps=$stepsAtTime, timestamp=$timestamp, lat=$latitude, lng=$longitude")
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
    
    /**
     * Gets selected cat skin (0-3), default is 0
     */
    fun getSelectedSkin(): Int {
        return prefs.getInt(KEY_SELECTED_SKIN, 0)
    }
    
    /**
     * Saves selected cat skin (0-3)
     */
    fun setSelectedSkin(skin: Int) {
        prefs.edit().putInt(KEY_SELECTED_SKIN, skin).apply()
        android.util.Log.d("UserPreferences", "Saved selected skin: $skin")
    }
    
    /**
     * Gets user nickname
     */
    fun getNickname(): String? {
        return prefs.getString(KEY_NICKNAME, null)
    }
    
    /**
     * Saves user nickname
     */
    fun setNickname(nickname: String?) {
        prefs.edit().putString(KEY_NICKNAME, nickname).apply()
        android.util.Log.d("UserPreferences", "Saved nickname: $nickname")
    }
    
    /**
     * Gets user age
     */
    fun getAge(): Int? {
        val age = prefs.getInt(KEY_AGE, -1)
        return if (age >= 0) age else null
    }
    
    /**
     * Saves user age
     */
    fun setAge(age: Int?) {
        prefs.edit().putInt(KEY_AGE, age ?: -1).apply()
        android.util.Log.d("UserPreferences", "Saved age: $age")
    }
    
    /**
     * Gets user gender
     */
    fun getGender(): String? {
        return prefs.getString(KEY_GENDER, null)
    }
    
    /**
     * Saves user gender
     */
    fun setGender(gender: String?) {
        prefs.edit().putString(KEY_GENDER, gender).apply()
        android.util.Log.d("UserPreferences", "Saved gender: $gender")
    }
    
    /**
     * Gets user location
     */
    fun getLocation(): String? {
        return prefs.getString(KEY_LOCATION, null)
    }
    
    /**
     * Saves user location
     */
    fun setLocation(location: String?) {
        prefs.edit().putString(KEY_LOCATION, location).apply()
        android.util.Log.d("UserPreferences", "Saved location: $location")
    }
    
    /**
     * Checks if initial setup is complete (user has selected skin and filled data)
     */
    fun isInitialSetupComplete(): Boolean {
        return prefs.getBoolean(KEY_INITIAL_SETUP_COMPLETE, false)
    }
    
    /**
     * Marks initial setup as complete
     */
    fun setInitialSetupComplete(complete: Boolean) {
        prefs.edit().putBoolean(KEY_INITIAL_SETUP_COMPLETE, complete).apply()
        android.util.Log.d("UserPreferences", "Set initial setup complete: $complete")
    }
    
    /**
     * Gets steps for current day (resets at midnight)
     * Calculates: today's steps = total steps - total steps at start of day
     */
    fun getTodayStepCount(): Int {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString(KEY_DAILY_STEP_DATE, null)
        val totalSteps = getTotalStepCount()
        
        // If date changed, reset daily tracking
        if (lastDate != today) {
            prefs.edit()
                .putString(KEY_DAILY_STEP_DATE, today)
                .putInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps)
                .putInt(KEY_DAILY_STEP_COUNT, 0)
                .apply()
            android.util.Log.d("UserPreferences", "New day detected - reset daily tracking. Total: $totalSteps")
            return 0
        }
        
        // Same day - calculate today's steps
        val totalAtStartOfDay = prefs.getInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps)
        val todaySteps = (totalSteps - totalAtStartOfDay).coerceAtLeast(0)
        
        // Update cached daily count
        prefs.edit().putInt(KEY_DAILY_STEP_COUNT, todaySteps).apply()
        
        return todaySteps
    }
    
    /**
     * Initializes daily step tracking when app starts
     * Should be called on app startup to ensure proper daily reset
     */
    fun initializeDailyStepTracking(totalSteps: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString(KEY_DAILY_STEP_DATE, null)
        
        if (lastDate != today) {
            // New day - reset and save starting point
            prefs.edit()
                .putString(KEY_DAILY_STEP_DATE, today)
                .putInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps)
                .putInt(KEY_DAILY_STEP_COUNT, 0)
                .apply()
            android.util.Log.d("UserPreferences", "Initialized new day - daily steps reset. Total: $totalSteps")
        } else {
            // Same day - ensure we have a starting point
            if (!prefs.contains(KEY_TOTAL_STEPS_AT_DAY_START)) {
                prefs.edit().putInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps).apply()
            }
        }
    }
}

