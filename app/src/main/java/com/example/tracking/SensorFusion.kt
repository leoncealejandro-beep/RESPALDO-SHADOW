package com.example.tracking

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.PI

class SensorFusion(context: Context) : SensorEventListener {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    
    private val _orientation = MutableStateFlow(OrientationData(0f, 0f, 0f))
    val orientation: StateFlow<OrientationData> = _orientation
    
    private val rotationMatrix = FloatArray(9)
    private val orientationValues = FloatArray(3)
    private var isSensorAvailable = false
    
    private var lastYaw = 0f
    private var cumulativeYaw = 0f
    
    data class OrientationData(
        val pitch: Float,
        val yaw: Float,
        val roll: Float
    )
    
    fun start() {
        if (rotationVectorSensor != null) {
            isSensorAvailable = true
            sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_GAME)
            Log.d("SensorFusion", "Sensor de rotación iniciado correctamente.")
        } else {
            Log.e("SensorFusion", "El sensor TYPE_ROTATION_VECTOR no está disponible en este dispositivo.")
        }
    }
    
    fun stop() {
        if (isSensorAvailable) {
            sensorManager.unregisterListener(this)
            isSensorAvailable = false
            Log.d("SensorFusion", "Sensor de rotación detenido.")
        }
    }
    
    fun reset() {
        _orientation.value = OrientationData(0f, 0f, 0f)
        lastYaw = 0f
        cumulativeYaw = 0f
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
            SensorManager.getOrientation(rotationMatrix, orientationValues)
            
            var pitch = Math.toDegrees(orientationValues[1].toDouble()).toFloat()
            var yaw = Math.toDegrees(orientationValues[0].toDouble()).toFloat()
            var roll = Math.toDegrees(orientationValues[2].toDouble()).toFloat()
            
            val yawDelta = yaw - lastYaw
            var normalizedDelta = yawDelta
            
            if (yawDelta > 180f) {
                normalizedDelta = yawDelta - 360f
            } else if (yawDelta < -180f) {
                normalizedDelta = yawDelta + 360f
            }
            
            cumulativeYaw += normalizedDelta
            lastYaw = yaw
            
            pitch = pitch.coerceIn(-85f, 85f)
            roll = roll.coerceIn(-45f, 45f)
            
            _orientation.value = OrientationData(pitch, cumulativeYaw, roll)
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            when (accuracy) {
                SensorManager.SENSOR_STATUS_UNRELIABLE ->
                    Log.w("SensorFusion", "Precisión del sensor: NO CONFIABLE")
                SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
                    Log.w("SensorFusion", "Precisión del sensor: BAJA (posible interferencia magnética)")
                SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
                    Log.d("SensorFusion", "Precisión del sensor: MEDIA")
                SensorManager.SENSOR_STATUS_ACCURACY_HIGH ->
                    Log.d("SensorFusion", "Precisión del sensor: ALTA")
            }
        }
    }
}