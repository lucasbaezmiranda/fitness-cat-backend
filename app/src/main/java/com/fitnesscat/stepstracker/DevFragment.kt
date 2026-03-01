package com.fitnesscat.stepstracker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.hardware.Sensor
import android.hardware.SensorManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationServices
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DevFragment : Fragment() {

    private lateinit var devStatusText: TextView
    private lateinit var saveLogsButton: Button
    private lateinit var exportStepsButton: Button

    // GPS views
    private lateinit var gpsPermissionDot: View
    private lateinit var gpsPermissionLabel: TextView
    private lateinit var gpsEnabledDot: View
    private lateinit var gpsEnabledLabel: TextView
    private lateinit var gpsLocationText: TextView

    // Sensor views
    private lateinit var stepSensorDot: View
    private lateinit var stepSensorLabel: TextView
    private lateinit var serviceDot: View
    private lateinit var serviceLabel: TextView

    // Permission views
    private lateinit var permActivityDot: View
    private lateinit var permActivityLabel: TextView
    private lateinit var permNotifDot: View
    private lateinit var permNotifLabel: TextView

    private val mainHandler = Handler(Looper.getMainLooper())
    private var statusUpdateRunnable: Runnable? = null
    private val STATUS_UPDATE_INTERVAL_MS = 2000L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dev, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        devStatusText = view.findViewById(R.id.devStatusText)
        saveLogsButton = view.findViewById(R.id.saveLogsButton)
        exportStepsButton = view.findViewById(R.id.exportStepsButton)

        // GPS
        gpsPermissionDot = view.findViewById(R.id.gpsPermissionDot)
        gpsPermissionLabel = view.findViewById(R.id.gpsPermissionLabel)
        gpsEnabledDot = view.findViewById(R.id.gpsEnabledDot)
        gpsEnabledLabel = view.findViewById(R.id.gpsEnabledLabel)
        gpsLocationText = view.findViewById(R.id.gpsLocationText)

        // Sensors
        stepSensorDot = view.findViewById(R.id.stepSensorDot)
        stepSensorLabel = view.findViewById(R.id.stepSensorLabel)
        serviceDot = view.findViewById(R.id.serviceDot)
        serviceLabel = view.findViewById(R.id.serviceLabel)

        // Permissions
        permActivityDot = view.findViewById(R.id.permActivityDot)
        permActivityLabel = view.findViewById(R.id.permActivityLabel)
        permNotifDot = view.findViewById(R.id.permNotifDot)
        permNotifLabel = view.findViewById(R.id.permNotifLabel)

        // Make dots circular
        listOf(gpsPermissionDot, gpsEnabledDot, stepSensorDot, serviceDot, permActivityDot, permNotifDot).forEach { dot ->
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(0xFFFF0000.toInt())
            dot.background = shape
        }

        saveLogsButton.setOnClickListener { saveLogsToFile() }
        exportStepsButton.setOnClickListener { exportStepFiles() }

        mainHandler.postDelayed({
            updateDebugStatus()
            startStatusUpdates()
        }, 500)
    }

    override fun onResume() {
        super.onResume()
        updateDebugStatus()
        startStatusUpdates()
    }

    override fun onPause() {
        super.onPause()
        statusUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statusUpdateRunnable?.let { mainHandler.removeCallbacks(it) }
    }

    private fun startStatusUpdates() {
        statusUpdateRunnable?.let { mainHandler.removeCallbacks(it) }

        statusUpdateRunnable = object : Runnable {
            override fun run() {
                updateDebugStatus()
                statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
            }
        }

        statusUpdateRunnable?.let { mainHandler.postDelayed(it, STATUS_UPDATE_INTERVAL_MS) }
    }

    private fun setDotColor(dot: View, green: Boolean) {
        val shape = dot.background as? GradientDrawable ?: GradientDrawable().also {
            it.shape = GradientDrawable.OVAL
            dot.background = it
        }
        shape.setColor(if (green) 0xFF4CAF50.toInt() else 0xFFFF0000.toInt())
    }

    private fun updateDebugStatus() {
        val context = requireContext()
        val userPreferences = (activity as? MainActivity)?.userPreferences ?: UserPreferences(context)

        // --- GPS ---
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasBackgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        setDotColor(gpsPermissionDot, hasFineLocation && hasBackgroundLocation)
        gpsPermissionLabel.text = when {
            hasFineLocation && hasBackgroundLocation -> "Permiso: OK (foreground + background)"
            hasFineLocation -> "Permiso: Solo foreground (falta background)"
            else -> "Permiso: DENEGADO"
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        setDotColor(gpsEnabledDot, gpsEnabled)
        gpsEnabledLabel.text = if (gpsEnabled) "GPS: Activado" else "GPS: DESACTIVADO"

        // Get last known location for display
        if (hasFineLocation) {
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        gpsLocationText.text = "Ubicacion: ${location.latitude}, ${location.longitude}"
                    } else {
                        gpsLocationText.text = "Ubicacion: sin fix (esperando GPS...)"
                    }
                }.addOnFailureListener {
                    gpsLocationText.text = "Ubicacion: error al obtener"
                }
            } catch (e: SecurityException) {
                gpsLocationText.text = "Ubicacion: sin permiso"
            }
        } else {
            gpsLocationText.text = "Ubicacion: sin permiso"
        }

        // --- Permissions ---
        val hasActivityRecognition = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
        } else true

        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true

        setDotColor(permActivityDot, hasActivityRecognition)
        permActivityLabel.text = if (hasActivityRecognition) "Activity Recognition: OK" else "Activity Recognition: DENEGADO"

        setDotColor(permNotifDot, hasNotificationPermission)
        permNotifLabel.text = if (hasNotificationPermission) "Notificaciones: OK" else "Notificaciones: DENEGADO"

        // --- Step sensor ---
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        setDotColor(stepSensorDot, stepCounterSensor != null)
        stepSensorLabel.text = if (stepCounterSensor != null) "Step Counter: Disponible" else "Step Counter: NO DISPONIBLE"

        // --- Service ---
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val serviceRunning = notificationManager.activeNotifications.any { it.id == 1 && it.tag == null }

        setDotColor(serviceDot, serviceRunning)
        serviceLabel.text = if (serviceRunning) "Servicio: Activo" else "Servicio: DETENIDO"

        // Try to restart service if not running and permissions are OK
        if (!serviceRunning && hasActivityRecognition && hasNotificationPermission && stepCounterSensor != null) {
            (activity as? MainActivity)?.startStepTrackingService()
        }

        // --- Info text ---
        val lastSensorValue = userPreferences.getLastSensorValue()
        val totalSteps = userPreferences.getTotalStepCount()
        val todaySteps = userPreferences.getTodayStepCount()
        val infoLines = mutableListOf<String>()
        infoLines.add("Android: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
        infoLines.add("Sensor value: $lastSensorValue")
        infoLines.add("Total steps: $totalSteps")
        infoLines.add("Today steps: $todaySteps")
        devStatusText.text = infoLines.joinToString("\n")
    }

    private fun saveLogsToFile() {
        try {
            val context = requireContext()
            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "fitness_cat_logs_$timestamp.txt"

            val statusText = devStatusText.text.toString()
            val logsText = AppLogger.getLogs()

            val fullContent = buildString {
                appendLine("=== Fitness Cat Debug Logs ===")
                appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                appendLine("Android Version: ${Build.VERSION.SDK_INT} (${Build.VERSION.RELEASE})")
                appendLine("")
                appendLine("=== Status ===")
                appendLine(statusText)
                appendLine("")
                appendLine("=== Logs ===")
                appendLine(logsText)
            }

            val cacheDir = context.cacheDir
            val file = File(cacheDir, fileName)

            FileWriter(file).use { writer ->
                writer.write(fullContent)
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fitness Cat Debug Logs")

                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file
                    )
                } else {
                    @Suppress("DEPRECATION")
                    android.net.Uri.fromFile(file)
                }

                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Share logs"))
            Toast.makeText(context, "File saved. Choose how to share it.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            android.util.Log.e("DevFragment", "Error saving logs: ${e.message}", e)
            Toast.makeText(requireContext(), "Error saving logs: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportStepFiles() {
        try {
            val context = requireContext()
            val stepsDir = context.getExternalFilesDir("steps")

            if (stepsDir == null || !stepsDir.exists()) {
                Toast.makeText(context, "No step files to export", Toast.LENGTH_SHORT).show()
                return
            }

            val stepFiles = stepsDir.listFiles()?.filter { it.name.startsWith("steps_") && it.name.endsWith(".txt") }

            if (stepFiles.isNullOrEmpty()) {
                Toast.makeText(context, "No step files to export", Toast.LENGTH_SHORT).show()
                return
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val combinedFileName = "steps_combined_$timestamp.txt"
            val cacheDir = context.cacheDir
            val combinedFile = File(cacheDir, combinedFileName)

            FileWriter(combinedFile).use { writer ->
                writer.appendLine("=== Fitness Cat Step Records ===")
                writer.appendLine("Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}")
                writer.appendLine("Format: timestamp,steps")
                writer.appendLine("")

                stepFiles.sortedBy { it.name }.forEach { file ->
                    writer.appendLine("--- File: ${file.name} ---")
                    file.readLines().forEach { line ->
                        writer.appendLine(line)
                    }
                    writer.appendLine("")
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Fitness Cat Step Records")

                val fileUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    androidx.core.content.FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        combinedFile
                    )
                } else {
                    @Suppress("DEPRECATION")
                    android.net.Uri.fromFile(combinedFile)
                }

                putExtra(Intent.EXTRA_STREAM, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            startActivity(Intent.createChooser(shareIntent, "Export steps"))
            Toast.makeText(context, "Combined file created. Choose how to share it.", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            android.util.Log.e("DevFragment", "Error exporting step files: ${e.message}", e)
            Toast.makeText(requireContext(), "Error exporting steps: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
