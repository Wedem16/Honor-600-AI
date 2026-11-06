package com.example.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import com.example.model.ThermalHealthLevel
import kotlin.math.atan2
import kotlin.math.sqrt

class DeviceEnvironmentMonitor(
    private val context: Context,
    private val onOrientationChanged: (rollDeg: Float, pitchDeg: Float) -> Unit,
    private val onThermalChanged: (level: ThermalHealthLevel, tempC: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private var isStarted = false
    private var currentTempC = 32.0f
    private var currentThermalLevel = ThermalHealthLevel.NOMINAL

    private val thermalListener: Any? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        PowerManager.OnThermalStatusChangedListener { status ->
            currentThermalLevel = mapPowerThermalStatus(status)
            onThermalChanged(currentThermalLevel, currentTempC)
        }
    } else {
        null
    }

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val tenths = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 320) ?: 320
            currentTempC = tenths / 10f
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                currentThermalLevel = when {
                    currentTempC >= 46f -> ThermalHealthLevel.CRITICAL
                    currentTempC >= 43f -> ThermalHealthLevel.SEVERE
                    currentTempC >= 39f -> ThermalHealthLevel.MODERATE
                    currentTempC >= 36f -> ThermalHealthLevel.LIGHT
                    else -> ThermalHealthLevel.NOMINAL
                }
            }
            onThermalChanged(currentThermalLevel, currentTempC)
        }
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener is PowerManager.OnThermalStatusChangedListener) {
            try {
                powerManager?.addThermalStatusListener(thermalListener)
            } catch (_: Exception) {
            }
        }

        try {
            context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        } catch (_: Exception) {
        }
    }

    fun stop() {
        if (!isStarted) return
        isStarted = false
        try {
            sensorManager?.unregisterListener(this)
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thermalListener is PowerManager.OnThermalStatusChangedListener) {
            try {
                powerManager?.removeThermalStatusListener(thermalListener)
            } catch (_: Exception) {
            }
        }
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            val gx = event.values[0]
            val gy = event.values[1]
            val gz = event.values[2]
            val norm = sqrt(gx * gx + gy * gy + gz * gz).coerceAtLeast(0.001f)
            val rollRad = atan2(-gx.toDouble(), gy.toDouble())
            val pitchRad = atan2(gz.toDouble(), norm.toDouble())
            val rollDeg = Math.toDegrees(rollRad).toFloat().coerceIn(-90f, 90f)
            val pitchDeg = Math.toDegrees(pitchRad).toFloat().coerceIn(-90f, 90f)
            onOrientationChanged(rollDeg, pitchDeg)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun mapPowerThermalStatus(status: Int): ThermalHealthLevel {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalHealthLevel.NOMINAL
        return when (status) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalHealthLevel.NOMINAL
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalHealthLevel.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalHealthLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalHealthLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalHealthLevel.CRITICAL
            else -> ThermalHealthLevel.NOMINAL
        }
    }
}
