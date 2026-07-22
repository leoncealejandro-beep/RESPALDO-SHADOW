package com.example.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorFusion(context: Context) {

    data class OrientationData(
        val pitch: Float,
        val yaw: Float,
        val roll: Float
    )

    private val sensorManager =
        context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val rotationVectorSensor: Sensor? =
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private val _orientation = MutableStateFlow(OrientationData(0f, 0f, 0f))
    val orientation: StateFlow<OrientationData> = _orientation

    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)

    private var isListening = false
    private var hasInitialReading = false

    private var offsetPitch = 0f
    private var offsetYaw = 0f
    private var offsetRoll = 0f

    private var lastRawYaw = 0f
    private var continuousYaw = 0f

    private val sensorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type != Sensor.TYPE_ROTATION_VECTOR) return

            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)

            val rawPitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
            val rawYaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            val rawRoll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()

            if (!hasInitialReading) {
                offsetPitch = rawPitch
                offsetYaw = rawYaw
                offsetRoll = rawRoll
                lastRawYaw = rawYaw
                continuousYaw = 0f
                hasInitialReading = true
                _orientation.value = OrientationData(0f, 0f, 0f)
                return
            }

            val pitch = rawPitch - offsetPitch
            val roll = rawRoll - offsetRoll

            val deltaYaw = rawYaw - lastRawYaw
            lastRawYaw = rawYaw

            continuousYaw += when {
                deltaYaw > 180f -> deltaYaw - 360f
                deltaYaw < -180f -> deltaYaw + 360f
                else -> deltaYaw
            }

            _orientation.value = OrientationData(pitch, continuousYaw, roll)
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
            if (sensor.type != Sensor.TYPE_ROTATION_VECTOR) return
            val label = when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE -> "NO CONFIABLE"
                SensorManager.SENSOR_STATUS_ACCURACY_LOW -> "BAJA (posible interferencia magnética)"
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM -> "MEDIA"
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH -> "ALTA"
                else -> "DESCONOCIDA ($accuracy)"
            }
            Log.d(TAG, "Precisión del sensor: $label")
        }
    }

    fun start() {
        val sensor = rotationVectorSensor
        if (sensor == null) {
            Log.e(TAG, "El sensor TYPE_ROTATION_VECTOR no está disponible en este dispositivo.")
            return
        }
        if (isListening) {
            Log.w(TAG, "El sensor ya está activo. Ignorando start() duplicado.")
            return
        }
        sensorManager.registerListener(sensorListener, sensor, SensorManager.SENSOR_DELAY_GAME)
        isListening = true
        Log.d(TAG, "Sensor de rotación iniciado correctamente.")
    }

    fun stop() {
        if (!isListening) return
        sensorManager.unregisterListener(sensorListener)
        isListening = false
        Log.d(TAG, "Sensor de rotación detenido.")
    }

    fun reset() {
        hasInitialReading = false
        continuousYaw = 0f
        _orientation.value = OrientationData(0f, 0f, 0f)
    }

    companion object {
        private const val TAG = "SensorFusion"
    }
}