package com.fitnesscat.stepstracker

import android.os.Handler
import android.os.Looper

/**
 * Simple logger that stores logs in memory for display in DevFragment
 */
object AppLogger {
    private val logBuffer = mutableListOf<String>()
    private const val MAX_LOG_LINES = 200
    private var logListener: ((String) -> Unit)? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    
    fun log(tag: String, message: String) {
        val timestamp = System.currentTimeMillis() % 100000 // Last 5 digits for brevity
        val logLine = "[$timestamp] $tag: $message"
        
        synchronized(logBuffer) {
            logBuffer.add(logLine)
            // Keep only last MAX_LOG_LINES
            if (logBuffer.size > MAX_LOG_LINES) {
                logBuffer.removeAt(0)
            }
        }
        
        // Also log to system logcat
        android.util.Log.d(tag, message)
        
        // Notify listener on main thread
        logListener?.let { listener ->
            mainHandler.post {
                listener(logLine)
            }
        }
    }
    
    fun getLogs(): String {
        synchronized(logBuffer) {
            return logBuffer.joinToString("\n")
        }
    }
    
    fun clearLogs() {
        synchronized(logBuffer) {
            logBuffer.clear()
        }
    }
    
    fun setLogListener(listener: ((String) -> Unit)?) {
        logListener = listener
    }
}


