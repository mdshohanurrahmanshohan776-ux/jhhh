package com.example.camera

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.atan2
import kotlin.math.roundToInt
import kotlin.math.sqrt

class LevelSensorHelper(context: Context, private val onAngleChanged: (rollAngle: Float, isLevel: Boolean) -> Unit) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var isListening = false

    fun start() {
        if (!isListening && accelerometer != null) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
            isListening = true
        }
    }

    fun stop() {
        if (isListening) {
            sensorManager?.unregisterListener(this)
            isListening = false
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return

        val ax = event.values[0]
        val ay = event.values[1]
        val az = event.values[2]

        // Calculate roll angle (tilt left/right in degrees)
        val roll = Math.toDegrees(atan2(-ax.toDouble(), ay.toDouble())).toFloat()
        
        // Normalize roll so horizontal phone orientation has 0 at landscape/portrait
        // In typical portrait orientation, ay is ~9.8 and ax is ~0 -> roll ~ 0
        var normalizedAngle = roll
        if (normalizedAngle > 90f) normalizedAngle -= 180f
        if (normalizedAngle < -90f) normalizedAngle += 180f

        val isLevel = kotlin.math.abs(normalizedAngle) <= 1.5f
        onAngleChanged(normalizedAngle, isLevel)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
