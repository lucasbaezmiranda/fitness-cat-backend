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
        private const val KEY_CURRENT_HEALTH = "current_health"  // Current health (0-80)
        const val MAX_HEALTH = 80
        const val INITIAL_HEALTH = 60
        private const val KEY_CURRENT_ACTIVITY_TYPE = "current_activity_type"  // Activity type constant
        private const val KEY_CURRENT_ACTIVITY_NAME = "current_activity_name"  // Activity name (human-readable)
        private const val KEY_CAT_PIEL    = "cat_piel"    // 0-3
        private const val KEY_CAT_MANCHAS = "cat_manchas"  // 0-3
        private const val KEY_CAT_OJOS    = "cat_ojos"     // 0-2
        private const val KEY_CAT_OREJAS  = "cat_orejas"   // 0-1
        private const val KEY_NICKNAME = "nickname"  // User nickname
        private const val KEY_AGE = "age"  // User age
        private const val KEY_GENDER = "gender"  // User gender
        private const val KEY_COUNTRY       = "country"        // User country
        private const val KEY_CITY          = "city"           // User city
        private const val KEY_URBAN_CONTEXT = "urban_context"  // User urban context
        private const val KEY_INITIAL_SETUP_COMPLETE = "initial_setup_complete"  // Whether initial setup is done
        private const val KEY_DAILY_STEP_COUNT = "daily_step_count"  // Steps for current day
        private const val KEY_DAILY_STEP_DATE = "daily_step_date"  // Date of last daily step reset (YYYY-MM-DD)
        private const val KEY_TOTAL_STEPS_AT_DAY_START = "total_steps_at_day_start"  // Total steps at start of current day
        private const val KEY_FCM_TOKEN = "fcm_token"  // Firebase Cloud Messaging token
        private const val KEY_PENDING_ACTIVITY_RECORDS = "pending_activity_records"  // JSON array of pending activity records
        // Activity accumulation keys
        private const val KEY_ACT_LAST_TYPE = "act_last_type"
        private const val KEY_ACT_LAST_TIMESTAMP = "act_last_timestamp"
        private const val KEY_ACT_MIN_WALKING = "act_min_walking"
        private const val KEY_ACT_MIN_RUNNING = "act_min_running"
        private const val KEY_ACT_MIN_ON_FOOT = "act_min_on_foot"
        private const val KEY_ACT_MIN_IN_VEHICLE = "act_min_in_vehicle"
        private const val KEY_ACT_MIN_ON_BICYCLE = "act_min_on_bicycle"
        private const val KEY_ACT_MIN_STILL = "act_min_still"
        private const val KEY_ACT_MIN_UNKNOWN = "act_min_unknown"
        private const val KEY_ACT_LAST_UPDATE_MS = "act_last_update_ms"  // System.currentTimeMillis of last AR callback
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
     * Gets current health (0-80), default is 60
     */
    fun getCurrentHealth(): Int {
        return prefs.getInt(KEY_CURRENT_HEALTH, INITIAL_HEALTH)
    }

    /**
     * Saves current health (0-80)
     */
    fun setCurrentHealth(health: Int) {
        prefs.edit().putInt(KEY_CURRENT_HEALTH, health).apply()
        android.util.Log.d("UserPreferences", "Saved current health: $health")
    }

    /**
     * Converts a health value to its corresponding cat stage.
     * 0      → stage 1
     * 1-20   → stage 2
     * 21-40  → stage 3
     * 41-60  → stage 4
     * 61-80  → stage 5
     */
    fun healthToStage(health: Int): Int = when {
        health <= 0  -> 1
        health <= 20 -> 2
        health <= 40 -> 3
        health <= 60 -> 4
        else         -> 5
    }

    /**
     * Applies the daily health delta based on yesterday's step count.
     * >5000 steps → +5 HP, <3000 steps → -3 HP, otherwise no change.
     * Health is clamped to [0, MAX_HEALTH].
     */
    private fun applyDailyHealthUpdate(yesterdaySteps: Int) {
        val delta = when {
            yesterdaySteps > 5000 -> 5
            yesterdaySteps < 3000 -> -3
            else -> 0
        }
        if (delta != 0) {
            val current = getCurrentHealth()
            val newHealth = (current + delta).coerceIn(0, MAX_HEALTH)
            setCurrentHealth(newHealth)
            setCurrentStage(healthToStage(newHealth))
            android.util.Log.d("UserPreferences",
                "Daily health update: yesterdaySteps=$yesterdaySteps, delta=$delta, $current -> $newHealth")
        }
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
        return prefs.getString(KEY_CURRENT_ACTIVITY_NAME, "Unknown") ?: "Unknown"
    }
    
    /**
     * Called when Activity Recognition detects a new activity.
     * Accumulates elapsed time for the previous activity, then updates current.
     */
    fun onActivityChanged(newType: Int, newName: String) {
        val now = System.currentTimeMillis() / 1000
        val lastType = prefs.getInt(KEY_ACT_LAST_TYPE, com.google.android.gms.location.DetectedActivity.UNKNOWN)
        val lastTimestamp = prefs.getLong(KEY_ACT_LAST_TIMESTAMP, 0L)

        if (lastTimestamp > 0) {
            val elapsedSeconds = now - lastTimestamp
            val elapsedMinutes = (elapsedSeconds / 60).toInt().coerceAtMost(60) // cap at 60min to avoid stale data
            if (elapsedMinutes > 0) {
                val key = getAccumulatorKey(lastType)
                val current = prefs.getInt(key, 0)
                prefs.edit().putInt(key, current + elapsedMinutes).apply()
            }
        }

        prefs.edit()
            .putInt(KEY_ACT_LAST_TYPE, newType)
            .putInt(KEY_CURRENT_ACTIVITY_TYPE, newType)
            .putString(KEY_CURRENT_ACTIVITY_NAME, newName)
            .putLong(KEY_ACT_LAST_TIMESTAMP, now)
            .putLong(KEY_ACT_LAST_UPDATE_MS, System.currentTimeMillis())
            .apply()
        android.util.Log.d("UserPreferences", "Activity changed: $newName (type: $newType)")
    }

    private fun getAccumulatorKey(activityType: Int): String {
        return when (activityType) {
            com.google.android.gms.location.DetectedActivity.WALKING -> KEY_ACT_MIN_WALKING
            com.google.android.gms.location.DetectedActivity.RUNNING -> KEY_ACT_MIN_RUNNING
            com.google.android.gms.location.DetectedActivity.ON_FOOT -> KEY_ACT_MIN_ON_FOOT
            com.google.android.gms.location.DetectedActivity.IN_VEHICLE -> KEY_ACT_MIN_IN_VEHICLE
            com.google.android.gms.location.DetectedActivity.ON_BICYCLE -> KEY_ACT_MIN_ON_BICYCLE
            com.google.android.gms.location.DetectedActivity.STILL -> KEY_ACT_MIN_STILL
            else -> KEY_ACT_MIN_UNKNOWN
        }
    }

    /**
     * Finalizes the current ongoing activity (adds elapsed time since last change)
     * and returns all accumulated minutes. Call this from StepWorker before resetting.
     */
    fun finalizeAndGetAccumulators(): Map<String, Int> {
        val now = System.currentTimeMillis() / 1000
        val currentType = prefs.getInt(KEY_ACT_LAST_TYPE, com.google.android.gms.location.DetectedActivity.UNKNOWN)
        val lastTimestamp = prefs.getLong(KEY_ACT_LAST_TIMESTAMP, 0L)

        // Finalize the current ongoing activity
        if (lastTimestamp > 0) {
            val elapsed = now - lastTimestamp
            val minutes = (elapsed / 60).toInt().coerceAtMost(30) // cap at 30 min (worker interval)
            if (minutes > 0) {
                val key = getAccumulatorKey(currentType)
                val current = prefs.getInt(key, 0)
                prefs.edit().putInt(key, current + minutes).apply()
            }
        }
        // Update last timestamp to now (next window starts here)
        prefs.edit().putLong(KEY_ACT_LAST_TIMESTAMP, now).apply()

        return mapOf(
            "walking_min" to prefs.getInt(KEY_ACT_MIN_WALKING, 0),
            "running_min" to prefs.getInt(KEY_ACT_MIN_RUNNING, 0),
            "on_foot_min" to prefs.getInt(KEY_ACT_MIN_ON_FOOT, 0),
            "in_vehicle_min" to prefs.getInt(KEY_ACT_MIN_IN_VEHICLE, 0),
            "on_bicycle_min" to prefs.getInt(KEY_ACT_MIN_ON_BICYCLE, 0),
            "still_min" to prefs.getInt(KEY_ACT_MIN_STILL, 0),
            "unknown_min" to prefs.getInt(KEY_ACT_MIN_UNKNOWN, 0)
        )
    }

    fun resetActivityAccumulators() {
        prefs.edit()
            .putInt(KEY_ACT_MIN_WALKING, 0)
            .putInt(KEY_ACT_MIN_RUNNING, 0)
            .putInt(KEY_ACT_MIN_ON_FOOT, 0)
            .putInt(KEY_ACT_MIN_IN_VEHICLE, 0)
            .putInt(KEY_ACT_MIN_ON_BICYCLE, 0)
            .putInt(KEY_ACT_MIN_STILL, 0)
            .putInt(KEY_ACT_MIN_UNKNOWN, 0)
            .apply()
        android.util.Log.d("UserPreferences", "Reset activity accumulators")
    }
    
    fun getLastActivityUpdateMs(): Long {
        return prefs.getLong(KEY_ACT_LAST_UPDATE_MS, 0L)
    }

    fun getCatPiel(): Int = prefs.getInt(KEY_CAT_PIEL, 0)
    fun setCatPiel(v: Int) { prefs.edit().putInt(KEY_CAT_PIEL, v).apply() }

    fun getCatManchas(): Int = prefs.getInt(KEY_CAT_MANCHAS, 0)
    fun setCatManchas(v: Int) { prefs.edit().putInt(KEY_CAT_MANCHAS, v).apply() }

    fun getCatOjos(): Int = prefs.getInt(KEY_CAT_OJOS, 0)
    fun setCatOjos(v: Int) { prefs.edit().putInt(KEY_CAT_OJOS, v).apply() }

    fun getCatOrejas(): Int = prefs.getInt(KEY_CAT_OREJAS, 0)
    fun setCatOrejas(v: Int) { prefs.edit().putInt(KEY_CAT_OREJAS, v).apply() }

    fun getCatCode(): String = "${getCatPiel()}${getCatManchas()}${getCatOjos()}${getCatOrejas()}"

    fun setCatCharacteristics(piel: Int, manchas: Int, ojos: Int, orejas: Int) {
        prefs.edit()
            .putInt(KEY_CAT_PIEL, piel)
            .putInt(KEY_CAT_MANCHAS, manchas)
            .putInt(KEY_CAT_OJOS, ojos)
            .putInt(KEY_CAT_OREJAS, orejas)
            .apply()
        android.util.Log.d("UserPreferences", "Saved cat characteristics: $piel$manchas$ojos$orejas")
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
     * Gets user age (as string, e.g. "25", "<14", ">50")
     */
    fun getAge(): String? {
        return prefs.getString(KEY_AGE, null)
    }

    /**
     * Saves user age (as string)
     */
    fun setAge(age: String?) {
        prefs.edit().putString(KEY_AGE, age).apply()
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
    
    fun getCountry(): String? = prefs.getString(KEY_COUNTRY, "Norway")
    fun setCountry(v: String?) { prefs.edit().putString(KEY_COUNTRY, v).apply() }

    fun getCity(): String? = prefs.getString(KEY_CITY, null)
    fun setCity(v: String?) { prefs.edit().putString(KEY_CITY, v).apply() }

    fun getUrbanContext(): String? = prefs.getString(KEY_URBAN_CONTEXT, null)
    fun setUrbanContext(v: String?) { prefs.edit().putString(KEY_URBAN_CONTEXT, v).apply() }
    
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
        
        // If date changed, apply health update for yesterday then reset
        if (lastDate != today) {
            if (lastDate != null) {
                val totalAtStartOfDay = prefs.getInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps)
                val yesterdaySteps = (totalSteps - totalAtStartOfDay).coerceAtLeast(0)
                applyDailyHealthUpdate(yesterdaySteps)
            }
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
    
    fun getFcmToken(): String? {
        return prefs.getString(KEY_FCM_TOKEN, null)
    }

    fun setFcmToken(token: String) {
        prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
        android.util.Log.d("UserPreferences", "Saved FCM token: ${token.take(10)}...")
    }

    /**
     * Initializes daily step tracking when app starts
     * Should be called on app startup to ensure proper daily reset
     */
    // --- Pending activity records ---

    fun getPendingActivityRecords(): String {
        return prefs.getString(KEY_PENDING_ACTIVITY_RECORDS, "[]") ?: "[]"
    }

    fun savePendingActivityRecords(jsonString: String) {
        prefs.edit().putString(KEY_PENDING_ACTIVITY_RECORDS, jsonString).apply()
    }

    fun addPendingActivityRecord(accumulators: Map<String, Int>, timestamp: Long) {
        try {
            val pendingJson = getPendingActivityRecords()

            val fields = accumulators.entries.joinToString(",") { (k, v) -> "\"$k\":$v" }
            val newRecord = "{\"timestamp\":$timestamp,$fields}"

            val updatedJson = if (pendingJson == "[]") {
                "[$newRecord]"
            } else {
                pendingJson.dropLast(1) + ",$newRecord]"
            }

            savePendingActivityRecords(updatedJson)
            android.util.Log.d("UserPreferences", "Added pending activity record: $newRecord")
        } catch (e: Exception) {
            android.util.Log.e("UserPreferences", "Error adding pending activity record: ${e.message}", e)
        }
    }

    fun clearPendingActivityRecords() {
        prefs.edit().putString(KEY_PENDING_ACTIVITY_RECORDS, "[]").apply()
        android.util.Log.d("UserPreferences", "Cleared all pending activity records")
    }

    fun initializeDailyStepTracking(totalSteps: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastDate = prefs.getString(KEY_DAILY_STEP_DATE, null)
        
        if (lastDate != today) {
            // Apply health update for the previous day before resetting
            if (lastDate != null) {
                val totalAtStartOfDay = prefs.getInt(KEY_TOTAL_STEPS_AT_DAY_START, totalSteps)
                val yesterdaySteps = (totalSteps - totalAtStartOfDay).coerceAtLeast(0)
                applyDailyHealthUpdate(yesterdaySteps)
            }
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

