package com.example.tracking

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarkerResult
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.acos
import kotlin.math.sqrt

// ============================================================================
// CONTRATO OBLIGATORIO: Clases de datos
// ============================================================================

data class Joint(val x: Float, val y: Float, val z: Float)

data class HandData(
    val isLeft: Boolean,
    val joints: List<Joint>,
    val gesture: HandTracker.GestureType,
    val pinchStrength: Float,
    val x: Float,
    val y: Float,
    val menuVisible: Boolean,
    val menuAnchorX: Float,
    val menuAnchorY: Float,
    val menuAnchorZ: Float
)

class HandTracker(private val context: Context) : ImageAnalysis.Analyzer {

    private val TAG = "HandTracker"

    // CONTRATO: GestureType debe contener exactamente estos valores
    enum class GestureType {
        NONE,
        OPEN_HAND,
        CLOSED_HAND,
        PINCH,
        POINTING,
        MENU
    }

    // CONTRATO: HandState debe contener exactamente estas propiedades
    data class HandState(
        val isDetected: Boolean = false,
        val x: Float = 0.5f,
        val y: Float = 0.5f,
        val gesture: GestureType = GestureType.NONE,
        val pinchStrength: Float = 0f,
        val hands: List<HandData> = emptyList()
    )

    private val _handState = MutableStateFlow(HandState())
    val handState: StateFlow<HandState> = _handState

    private var handLandmarker: HandLandmarker? = null

    @Volatile private var latestSensorW = 0
    @Volatile private var latestSensorH = 0
    @Volatile private var latestRotation = 0

    // Suavizado (interpolación) para evitar vibraciones
    private val smoothingAlpha = 0.75f 
    private val leftJointsBuffer = FloatArray(63)
    private val rightJointsBuffer = FloatArray(63)
    private val prevLeftJoints = FloatArray(63)
    private val prevRightJoints = FloatArray(63)
    
    private var hasPrevLeft = false
    private var hasPrevRight = false
    
    private var prevSmoothX = 0.5f
    private var prevSmoothY = 0.5f
    private var isHandCurrentlyDetected = false

    @Volatile private var isProcessing = false

    // ========================================================================
    // MATEMÁTICAS 3D REUTILIZABLES (Para orientación y gestos invariantes)
    // ========================================================================
    
    data class Vec3(val x: Float, val y: Float, val z: Float) {
        operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
        operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
        operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
        
        fun dot(other: Vec3): Float = x * other.x + y * other.y + z * other.z
        
        fun cross(other: Vec3): Vec3 = Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
        
        fun length(): Float = sqrt(x * x + y * y + z * z)
        
        fun normalize(): Vec3 {
            val len = length()
            return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 0f)
        }
    }

    fun angleBetween(a: Vec3, b: Vec3): Float {
        val dot = a.normalize().dot(b.normalize())
        return acos(dot.coerceIn(-1f, 1f))
    }

    // ========================================================================
    // ESTABILIDAD: Histéresis
    // ========================================================================
    
    private class GestureHysteresis {
        var activeGesture: GestureType = GestureType.NONE
        private var rawGesture: GestureType = GestureType.NONE
        private var rawGestureStartMs = 0L
        private var gestureLostMs = 0L

        fun update(detected: GestureType, now: Long): GestureType {
            if (detected != GestureType.NONE) {
                if (detected == rawGesture) {
                    // Mantener durante 150 ms antes de activarse
                    if (now - rawGestureStartMs >= 150L) {
                        activeGesture = detected
                    }
                } else {
                    rawGesture = detected
                    rawGestureStartMs = now
                }
                gestureLostMs = 0L // Resetear tiempo de pérdida si se detecta algo
            } else {
                // Debe mantenerse perdido durante 100 ms antes de desactivarse
                if (activeGesture != GestureType.NONE) {
                    if (gestureLostMs == 0L) gestureLostMs = now
                    if (now - gestureLostMs >= 100L) {
                        activeGesture = GestureType.NONE
                        gestureLostMs = 0L
                        rawGesture = GestureType.NONE
                    }
                }
            }
            
            // Si el gesto detectado coincide con el activo, estabilizamos
            if (detected == activeGesture) {
                gestureLostMs = 0L
            }
            
            return activeGesture
        }

        fun reset() {
            activeGesture = GestureType.NONE
            rawGesture = GestureType.NONE
            rawGestureStartMs = 0L
            gestureLostMs = 0L
        }
    }
    
    private val hysteresis = GestureHysteresis()

    // ========================================================================
    // INICIALIZACIÓN
    // ========================================================================

    init {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("hand_landmarker.task")
                .build()

            val options = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(baseOptions)
                .setMinHandDetectionConfidence(0.5f)
                .setMinTrackingConfidence(0.5f)
                .setMinHandPresenceConfidence(0.5f)
                .setRunningMode(RunningMode.LIVE_STREAM)
                .setNumHands(2)
                .setResultListener { result, _ -> 
                    processResultFast(result)
                    isProcessing = false
                }
                .setErrorListener { error ->
                    Log.e(TAG, "Error MP: ${error.message}")
                    isProcessing = false
                }
                .build()

            handLandmarker = HandLandmarker.createFromOptions(context, options)
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando HandLandmarker", e)
        }
    }

    // ========================================================================
    // ANÁLISIS DE IMAGEN
    // ========================================================================

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        if (isProcessing) {
            image.close()
            return
        }

        val landmarker = handLandmarker
        if (landmarker == null) {
            image.close()
            return
        }

        isProcessing = true
        latestSensorW = image.width
        latestSensorH = image.height
        latestRotation = image.imageInfo.rotationDegrees

        try {
            val bitmap = image.toBitmapSafe()
            val mpImage = BitmapImageBuilder(bitmap).build()
            landmarker.detectAsync(mpImage, System.currentTimeMillis())
        } catch (e: Exception) {
            Log.e(TAG, "Excepción frame", e)
            isProcessing = false
        } finally {
            image.close()
        }
    }

    // ========================================================================
    // PROCESAMIENTO DE RESULTADOS
    // ========================================================================

    private fun processResultFast(result: HandLandmarkerResult) {
        val landmarksList = result.landmarks()
        val now = System.currentTimeMillis()
        
        if (landmarksList.isEmpty()) {
            hasPrevLeft = false
            hasPrevRight = false
            isHandCurrentlyDetected = false
            
            // Alimentar NONE a la histéresis para manejar la desactivación retardada
            val stableGesture = hysteresis.update(GestureType.NONE, now)
            
            _handState.value = HandState(
                isDetected = false, 
                gesture = stableGesture,
                hands = emptyList()
            )
            return
        }

        isHandCurrentlyDetected = true
        val handsData = mutableListOf<HandData>()
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        var primaryHandData: HandData? = null

        for (i in landmarksList.indices) {
            val handLandmarks = landmarksList[i]
            val isLeft = result.handedness()[i].firstOrNull()?.categoryName()?.lowercase(Locale.ROOT) == "left"
            
            val currentBuffer = if (isLeft) leftJointsBuffer else rightJointsBuffer
            val prevBuffer = if (isLeft) prevLeftJoints else prevRightJoints
            val hasPrev = if (isLeft) hasPrevLeft else hasPrevRight

            // 1. Suavizado de joints (interpolación)
            for (j in 0 until 21) {
                val lm = handLandmarks[j]
                val idx = j * 3
                currentBuffer[idx] = lm.x()
                currentBuffer[idx + 1] = lm.y()
                currentBuffer[idx + 2] = lm.z()
            }

            if (hasPrev) {
                for (k in currentBuffer.indices) {
                    currentBuffer[k] = smoothingAlpha * currentBuffer[k] + (1f - smoothingAlpha) * prevBuffer[k]
                }
            }
            if (isLeft) hasPrevLeft = true else hasPrevRight = true
            currentBuffer.copyInto(prevBuffer)

            // 2. Conversión a Vec3 para matemáticas 3D robustas
            val landmarks3D = (0 until 21).map { 
                val idx = it * 3
                Vec3(currentBuffer[idx], currentBuffer[idx + 1], currentBuffer[idx + 2])
            }

            // 3. Detección de gestos usando orientación de la palma y vectores 3D
            val gestureResult = detectGesture3D(landmarks3D)
            val rawGesture = gestureResult.first
            val pinchVal = gestureResult.second

            // 4. Aplicar histéresis al gesto de la mano primaria (la primera detectada)
            val stableGesture = if (i == 0) {
                hysteresis.update(rawGesture, now)
            } else {
                rawGesture // Para manos secundarias, usamos el gesto crudo o podríamos tener otra histéresis
            }

            // 5. Cálculo de posición y anclaje del menú
            var menuVisible = false
            var menuAnchorX = 0f
            var menuAnchorY = 0f
            var menuAnchorZ = 0f

            if (stableGesture == GestureType.MENU) {
                menuVisible = true
                // Punto medio entre Landmark 4 (pulgar) y Landmark 8 (índice)
                val lm4 = landmarks3D[4]
                val lm8 = landmarks3D[8]
                menuAnchorX = (lm4.x + lm8.x) / 2f
                menuAnchorY = (lm4.y + lm8.y) / 2f
                menuAnchorZ = (lm4.z + lm8.z) / 2f
            }

            // 6. Mapeo de coordenadas a pantalla
            val targetIdx = if (stableGesture == GestureType.PINCH || stableGesture == GestureType.MENU) -1 else 8
            var finalX: Float
            var finalY: Float
            
            if (targetIdx == -1) {
                finalX = (currentBuffer[4 * 3] + currentBuffer[8 * 3]) / 2f
                finalY = (currentBuffer[4 * 3 + 1] + currentBuffer[8 * 3 + 1]) / 2f
            } else {
                finalX = currentBuffer[targetIdx * 3]
                finalY = currentBuffer[targetIdx * 3 + 1]
            }

            val mapped = mapCoordsFast(finalX, finalY, latestSensorW, latestSensorH, latestRotation, screenW, screenH)
            
            // Suavizado de la posición final para evitar vibraciones
            val smoothX = if (!isHandCurrentlyDetected) mapped.first else smoothingAlpha * mapped.first + (1f - smoothingAlpha) * prevSmoothX
            val smoothY = if (!isHandCurrentlyDetected) mapped.second else smoothingAlpha * mapped.second + (1f - smoothingAlpha) * prevSmoothY
            prevSmoothX = smoothX
            prevSmoothY = smoothY

            val joints = (0 until 21).map { 
                val idx = it * 3
                Joint(currentBuffer[idx], currentBuffer[idx + 1], currentBuffer[idx + 2])
            }

            val handData = HandData(
                isLeft = isLeft,
                joints = joints,
                gesture = stableGesture,
                pinchStrength = pinchVal,
                x = smoothX,
                y = smoothY,
                menuVisible = menuVisible,
                menuAnchorX = menuAnchorX,
                menuAnchorY = menuAnchorY,
                menuAnchorZ = menuAnchorZ
            )
            
            handsData.add(handData)
            
            if (i == 0) {
                primaryHandData = handData
            }
        }

        // 7. Actualizar estado global
        val primary = primaryHandData ?: HandData(
            isLeft = false,
            joints = emptyList(),
            gesture = GestureType.NONE,
            pinchStrength = 0f,
            x = 0.5f,
            y = 0.5f,
            menuVisible = false,
            menuAnchorX = 0f,
            menuAnchorY = 0f,
            menuAnchorZ = 0f
        )

        _handState.value = HandState(
            isDetected = true,
            x = primary.x,
            y = primary.y,
            gesture = primary.gesture,
            pinchStrength = primary.pinchStrength,
            hands = handsData
        )
    }

    // ========================================================================
    // LÓGICA DE GESTOS 3D (Invatante a rotación y ángulo de visión)
    // ========================================================================

    private fun detectGesture3D(l: List<Vec3>): Pair<GestureType, Float> {
        val wrist = l[0]
        
        // Orientación de la palma: normal al plano formado por muñeca, MCP índice (5), MCP meñique (17)
        // Esto establece un sistema de coordenadas local relativo a la mano, no a la pantalla.
        val vWristToIndexMcp = l[5] - wrist
        val vWristToPinkyMcp = l[17] - wrist
        val palmNormal = vWristToIndexMcp.cross(vWristToPinkyMcp).normalize()

        // Función reutilizable para verificar extensión de un dedo usando geometría 3D
        fun isExtended(mcpIdx: Int, tipIdx: Int): Boolean {
            val vMcp = l[mcpIdx] - wrist
            val vTip = l[tipIdx] - wrist
            val lenMcp = vMcp.length()
            val lenTip = vTip.length()
            
            if (lenMcp < 1e-5f) return false
            
            // El producto punto entre los vectores normalizados nos da el coseno del ángulo.
            // Si el dedo está extendido, apunta en la misma dirección general que el vector hacia el MCP.
            val dot = vMcp.normalize().dot(vTip.normalize())
            
            // Criterios robustos invariantes a la rotación:
            // 1. La punta está significativamente más lejos de la muñeca que el MCP.
            // 2. El ángulo entre el vector del MCP y el de la punta es menor a ~60 grados (cos > 0.5).
            return lenTip > lenMcp * 1.15f && dot > 0.5f
        }

        // Evaluación de cada dedo
        val thumbExt = isExtended(2, 4)
        val indexExt = isExtended(5, 8)
        val middleExt = isExtended(9, 12)
        val ringExt = isExtended(13, 16)
        val pinkyExt = isExtended(17, 20)

        // Cálculo de fuerza de pellizco (PINCH)
        val distThumbIndex = (l[4] - l[8]).length()
        val pinchStrength = (1f - (distThumbIndex - 0.02f) / 0.08f).coerceIn(0f, 1f)
        val isPinch = distThumbIndex < 0.06f

        if (isPinch) {
            return Pair(GestureType.PINCH, pinchStrength)
        }

        // GESTO MENU (Pistola): Pulgar e índice extendidos, medio, anular y meñique doblados.
        // Al usar vectores relativos a la muñeca y la normal de la palma, esto funciona desde cualquier ángulo visible.
        if (thumbExt && indexExt && !middleExt && !ringExt && !pinkyExt) {
            return Pair(GestureType.MENU, 0f)
        }

        // POINTING: Solo índice extendido
        if (indexExt && !middleExt && !ringExt && !pinkyExt) {
            return Pair(GestureType.POINTING, 0f)
        }

        // CLOSED_HAND: Ningún dedo extendido
        if (!thumbExt && !indexExt && !middleExt && !ringExt && !pinkyExt) {
            return Pair(GestureType.CLOSED_HAND, 0f)
        }

        // OPEN_HAND: Todos los dedos principales extendidos
        if (indexExt && middleExt && ringExt && pinkyExt) {
            return Pair(GestureType.OPEN_HAND, 0f)
        }

        return Pair(GestureType.NONE, 0f)
    }

    // ========================================================================
    // UTILIDADES
    // ========================================================================

    private fun mapCoordsFast(sx: Float, sy: Float, sW: Int, sH: Int, rot: Int, scW: Float, scH: Float): Pair<Float, Float> {
        var rx = sx
        var ry = sy
        val rW: Float
        val rH: Float

        when (rot) {
            90 -> { rx = 1.0f - sy; ry = sx; rW = sH.toFloat(); rH = sW.toFloat() }
            180 -> { rx = 1.0f - sx; ry = 1.0f - sy; rW = sW.toFloat(); rH = sH.toFloat() }
            270 -> { rx = sy; ry = 1.0f - sx; rW = sH.toFloat(); rH = sW.toFloat() }
            else -> { rx = sx; ry = sy; rW = sW.toFloat(); rH = sH.toFloat() }
        }

        val cAspect = scW / scH
        val iAspect = rW / rH
        var px = rx
        var py = ry

        if (iAspect > cAspect) {
            val scale = scH / rH
            val scaledWidth = rW * scale
            val offsetX = (scaledWidth - scW) / 2f
            px = (rx * scaledWidth - offsetX) / scW
        } else {
            val scale = scW / rW
            val scaledHeight = rH * scale
            val offsetY = (scaledHeight - scH) / 2f
            py = (ry * scaledHeight - offsetY) / scH
        }

        return Pair(px.coerceIn(0f, 1f), py.coerceIn(0f, 1f))
    }

    private fun ImageProxy.toBitmapSafe(): Bitmap {
        if (format == ImageFormat.YUV_420_888) {
            val yBuffer = planes[0].buffer.duplicate().apply { rewind() }
            val uBuffer = planes[1].buffer.duplicate().apply { rewind() }
            val vBuffer = planes[2].buffer.duplicate().apply { rewind() }

            val ySize = yBuffer.remaining()
            val uSize = uBuffer.remaining()
            val vSize = vBuffer.remaining()

            val nv21 = ByteArray(ySize + uSize + vSize)
            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, this.width, this.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, yuvImage.width, yuvImage.height), 100, out)
            val imageBytes = out.toByteArray()
            
            return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size) 
                ?: throw IllegalStateException("No se pudo decodificar Bitmap")
        } else {
            val bitmapBuffer = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = planes[0].buffer.duplicate().apply { rewind() }
            bitmapBuffer.copyPixelsFromBuffer(buffer)
            return bitmapBuffer
        }
    }
}