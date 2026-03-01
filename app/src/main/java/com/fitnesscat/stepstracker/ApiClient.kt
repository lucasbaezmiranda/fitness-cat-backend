package com.fitnesscat.stepstracker

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * API Client for syncing step data to AWS API Gateway endpoint
 * 
 * CONFIGURATION:
 * 1. Set your API Gateway endpoint URL in API_ENDPOINT
 * 2. Add any required headers (API keys, etc.) in createRequest()
 * 3. Adjust request body format if needed in syncSteps()
 */
class ApiClient {
    
    companion object {
        // API Gateway endpoint URL (steps — existing API)
        private const val API_ENDPOINT = "https://qdt4w3wkfj.execute-api.us-east-1.amazonaws.com/prod/steps"

        // New API Gateway endpoint (user profiles & notifications)
        // TODO: Update after running `terraform apply` in infra/
        private const val NEW_API_BASE = "https://4szvbbn7rh.execute-api.us-east-1.amazonaws.com/prod"

        // Optional: Add API key or other authentication headers
        private const val API_KEY = "" // Leave empty if not needed
        
        private const val TAG = "ApiClient"
        private const val TIMEOUT_SECONDS = 30L
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build()
    
    /**
     * Syncs step count to the API endpoint
     * 
     * @param userId User ID from UserPreferences
     * @param stepCount Current total step count
     * @param timestamp Current timestamp in milliseconds
     * @param latitude GPS latitude (optional)
     * @param longitude GPS longitude (optional)
     * @param callback Optional callback to handle success/failure
     */
    fun syncSteps(
        userId: String,
        stepCount: Int,
        timestamp: Long,
        latitude: Double? = null,
        longitude: Double? = null,
        callback: ((Boolean, String?) -> Unit)? = null
    ) {
        // Check if endpoint is configured
        if (API_ENDPOINT == "YOUR_API_GATEWAY_ENDPOINT_URL_HERE") {
            Log.w(TAG, "API endpoint not configured. Please set API_ENDPOINT in ApiClient.kt")
            callback?.invoke(false, "API endpoint not configured")
            return
        }
        
        // Run on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Convert timestamp from milliseconds to seconds (Unix timestamp)
                val timestampSeconds = timestamp / 1000
                
                // Create JSON request body matching API format
                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    put("steps", stepCount)
                    put("timestamp", timestampSeconds)
                    if (latitude != null) put("latitude", latitude)
                    if (longitude != null) put("longitude", longitude)
                }
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                
                // Create request with headers
                val request = createRequest(API_ENDPOINT, requestBody)
                
                // Execute request
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    
                    if (response.isSuccessful) {
                        // Check if response contains {"ok": true}
                        try {
                            val jsonResponse = JSONObject(responseBody ?: "{}")
                            val isOk = jsonResponse.optBoolean("ok", false)
                            
                            if (isOk) {
                                Log.d(TAG, "Successfully synced steps: $stepCount for user: $userId")
                                callback?.invoke(true, null)
                            } else {
                                val errorMsg = "API returned ok=false: $responseBody"
                                Log.e(TAG, "Failed to sync steps: $errorMsg")
                                callback?.invoke(false, errorMsg)
                            }
                        } catch (e: Exception) {
                            // Response is successful but JSON parsing failed - still consider it success
                            Log.w(TAG, "Could not parse response JSON, but HTTP status is successful: $responseBody")
                            Log.d(TAG, "Successfully synced steps: $stepCount for user: $userId")
                            callback?.invoke(true, null)
                        }
                    } else {
                        val errorMsg = "HTTP ${response.code}: $responseBody"
                        Log.e(TAG, "Failed to sync steps: $errorMsg")
                        callback?.invoke(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing steps: ${e.message}", e)
                callback?.invoke(false, e.message)
            }
        }
    }
    
    /**
     * Syncs multiple step records in batch to the API endpoint
     * 
     * @param userId User ID from UserPreferences
     * @param recordsJsonString JSON array string, format: [{"steps": 100, "timestamp": 1704123456, "latitude": 40.7128, "longitude": -74.0060}, ...]
     * @param callback Optional callback to handle success/failure
     */
    fun syncStepsBatch(
        userId: String,
        recordsJsonString: String,
        callback: ((Boolean, String?) -> Unit)? = null
    ) {
        // Check if endpoint is configured
        if (API_ENDPOINT == "YOUR_API_GATEWAY_ENDPOINT_URL_HERE") {
            Log.w(TAG, "API endpoint not configured. Please set API_ENDPOINT in ApiClient.kt")
            callback?.invoke(false, "API endpoint not configured")
            return
        }
        
        // Check if records array is empty
        if (recordsJsonString.isEmpty() || recordsJsonString == "[]") {
            Log.d(TAG, "No records to sync in batch")
            callback?.invoke(true, null)
            return
        }
        
        // Run on background thread
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Construir JSON final como string directamente
                // Format: {"user_id": "...", "records": [...]}
                val jsonString = "{\"user_id\":\"$userId\",\"records\":$recordsJsonString}"
                
                Log.d(TAG, "Sending batch sync for user: $userId")
                Log.d(TAG, "JSON body: $jsonString")
                
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonString.toRequestBody(mediaType)
                
                // Create request with headers
                val request = createRequest(API_ENDPOINT, requestBody)
                
                // Execute request
                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    
                    Log.d(TAG, "Response: HTTP ${response.code} - $responseBody")
                    
                    if (response.isSuccessful) {
                        // Verificar si contiene "ok":true (más simple que parsear JSON completo)
                        if (responseBody?.contains("\"ok\":true") == true || responseBody?.contains("\"ok\": true") == true) {
                            Log.d(TAG, "✓ Successfully synced batch for user: $userId")
                            callback?.invoke(true, null)
                        } else {
                            val errorMsg = "API returned ok=false: $responseBody"
                            Log.e(TAG, "✗ Failed to sync batch: $errorMsg")
                            callback?.invoke(false, errorMsg)
                        }
                    } else {
                        val errorMsg = "HTTP ${response.code}: $responseBody"
                        Log.e(TAG, "✗ Failed to sync batch: $errorMsg")
                        callback?.invoke(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error syncing batch: ${e.message}", e)
                callback?.invoke(false, e.message)
            }
        }
    }
    
    /**
     * Syncs user profile to the new API endpoint (DynamoDB)
     */
    fun syncUserProfile(
        userId: String,
        nickname: String?,
        age: String?,
        gender: String?,
        country: String?,
        city: String?,
        urbanContext: String?,
        fcmToken: String? = null,
        callback: ((Boolean, String?) -> Unit)? = null
    ) {
        if (NEW_API_BASE.contains("PLACEHOLDER")) {
            Log.w(TAG, "New API base URL not configured. Update NEW_API_BASE in ApiClient.kt")
            callback?.invoke(false, "New API endpoint not configured")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonBody = JSONObject().apply {
                    put("user_id", userId)
                    if (nickname != null) put("nickname", nickname)
                    if (age != null) put("age", age)
                    if (gender != null) put("gender", gender)
                    if (country != null) put("country", country)
                    if (city != null) put("city", city)
                    if (urbanContext != null) put("urban_context", urbanContext)
                    if (fcmToken != null) put("fcm_token", fcmToken)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonBody.toString().toRequestBody(mediaType)
                val request = createRequest("$NEW_API_BASE/user-profile", requestBody)

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    if (response.isSuccessful) {
                        Log.d(TAG, "Successfully synced user profile for user: $userId")
                        callback?.invoke(true, null)
                    } else {
                        val errorMsg = "HTTP ${response.code}: $responseBody"
                        Log.e(TAG, "Failed to sync user profile: $errorMsg")
                        callback?.invoke(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing user profile: ${e.message}", e)
                callback?.invoke(false, e.message)
            }
        }
    }

    /**
     * Syncs activity + speed records in batch to the activity endpoint
     *
     * @param userId User ID from UserPreferences
     * @param recordsJsonString JSON array string with activity records
     * @param callback Optional callback to handle success/failure
     */
    fun syncActivityBatch(
        userId: String,
        recordsJsonString: String,
        callback: ((Boolean, String?) -> Unit)? = null
    ) {
        if (recordsJsonString.isEmpty() || recordsJsonString == "[]") {
            Log.d(TAG, "No activity records to sync")
            callback?.invoke(true, null)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val jsonString = "{\"user_id\":\"$userId\",\"records\":$recordsJsonString}"

                Log.d(TAG, "Sending activity batch sync for user: $userId")
                Log.d(TAG, "JSON body: $jsonString")

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val requestBody = jsonString.toRequestBody(mediaType)
                val request = createRequest("$NEW_API_BASE/activity", requestBody)

                client.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Activity response: HTTP ${response.code} - $responseBody")

                    if (response.isSuccessful) {
                        Log.d(TAG, "✓ Successfully synced activity batch for user: $userId")
                        callback?.invoke(true, null)
                    } else {
                        val errorMsg = "HTTP ${response.code}: $responseBody"
                        Log.e(TAG, "✗ Failed to sync activity batch: $errorMsg")
                        callback?.invoke(false, errorMsg)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "✗ Error syncing activity batch: ${e.message}", e)
                callback?.invoke(false, e.message)
            }
        }
    }

    /**
     * Creates the HTTP request with headers
     * Modify this method to add authentication headers, API keys, etc.
     */
    private fun createRequest(url: String, requestBody: okhttp3.RequestBody): Request {
        val requestBuilder = Request.Builder()
            .url(url)
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
        
        // Add API key if configured
        if (API_KEY.isNotEmpty()) {
            requestBuilder.addHeader("x-api-key", API_KEY)
            // Or use Authorization header:
            // requestBuilder.addHeader("Authorization", "Bearer $API_KEY")
        }
        
        // Add any other required headers here
        // Example for AWS API Gateway with API key:
        // requestBuilder.addHeader("x-api-key", "your-api-key-here")
        
        return requestBuilder.build()
    }
}

