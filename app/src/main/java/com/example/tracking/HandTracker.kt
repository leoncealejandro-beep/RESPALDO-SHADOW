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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.ByteArrayOutputStream
import java.util.Locale
import kotlin.math.sqrt

// Sacamos las clases al nivel superior para que sean accesibles por RenderHandSkeletons
data class Joint(val x: Float, val y: Float, val z: Float)

data class HandData(
    val isLeft: Boolean,
    val joints: List<Joint>,
    val gesture: HandTracker.GestureType,
    val pinchStrength: Float,
    val x: Float, // Coordenada X normalizada 0..1 (mapeada a pantalla)
    val y: Float  // Coordenada Y normalizada 0..1 (mapeada a pantalla)
)

class HandTracker(private val context: Context) : ImageAnalysis.Analyzer {

    private val TAG = "HandTracker"

    enum class GestureType {
        NONE, OPEN_HAND, CLOSED_HAND, PINCH, POINTING
    }

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

    private val smoothingAlpha = 0.8f 
    private val leftJointsBuffer = FloatArray(63)
    private val rightJointsBuffer = FloatArray(63)
    private val prevLeftJoints = FloatArray(63)
    private val prevRightJoints = FloatArray(63)
    
    private var hasPrevLeft = false
    private var hasPrevRight = false
    @Volatile private var isProcessing = false

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

    private fun processResultFast(result: HandLandmarkerResult) {
        val landmarksList = result.landmarks()
        
        if (landmarksList.isEmpty()) {
            hasPrevLeft = false
            hasPrevRight = false
            _handState.value = HandState(isDetected = false, hands = emptyList())
            return
        }

        val handsData = mutableListOf<HandData>()
        val metrics = context.resources.displayMetrics
        val screenW = metrics.widthPixels.toFloat()
        val screenH = metrics.heightPixels.toFloat()

        for (i in landmarksList.indices) {
            val handLandmarks = landmarksList[i]
            val isLeft = result.handedness()[i].firstOrNull()?.categoryName()?.lowercase(Locale.ROOT) == "left"
            
            val currentBuffer = if (isLeft) leftJointsBuffer else rightJointsBuffer
            val prevBuffer = if (isLeft) prevLeftJoints else prevRightJoints
            val hasPrev = if (isLeft) hasPrevLeft else hasPrevRight

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

            // Cálculo Gestos
            val tx = currentBuffer[4*3]; val ty = currentBuffer[4*3+1]
            val ix = currentBuffer[8*3]; val iy = currentBuffer[8*3+1]
            
            val dx = tx - ix; val dy = ty - iy
            val distSq = dx*dx + dy*dy
            val isPinch = distSq < 0.0025f 
            
            var gesture = GestureType.OPEN_HAND
            var pinchVal = 0f

            if (isPinch) {
                gesture = GestureType.PINCH
                pinchVal = (1f - (sqrt(distSq) - 0.04f) / 0.12f).coerceIn(0f, 1f)
            } else {
                fun isExtended(tipIdx: Int, mcpIdx: Int): Boolean {
                    val tIdx = tipIdx * 3; val mIdx = mcpIdx * 3
                    val wx = currentBuffer[0]; val wy = currentBuffer[1]
                    val distTip = (currentBuffer[tIdx]-wx).let { it*it } + (currentBuffer[tIdx+1]-wy).let { it*it }
                    val distMcp = (currentBuffer[mIdx]-wx).let { it*it } + (currentBuffer[mIdx+1]-wy).let { it*it }
                    return distTip > distMcp * 1.5f
                }
                val indexExt = isExtended(8, 5)
                val middleExt = isExtended(12, 9)
                
                if (indexExt && !middleExt) gesture = GestureType.POINTING
                else if (!indexExt && !middleExt && !isExtended(16, 13) && !isExtended(20, 17)) gesture = GestureType.CLOSED_HAND
            }

            val joints = mutableListOf<Joint>()
            for (j in 0 until 21) {
                val idx = j * 3
                joints.add(Joint(currentBuffer[idx], currentBuffer[idx+1], currentBuffer[idx+2]))
            }

            val targetIdx = if (gesture == GestureType.PINCH) -1 else 8
            var finalX: Float
            var finalY: Float
            
            if (targetIdx == -1) {
                finalX = (tx + ix) / 2f
                finalY = (ty + iy) / 2f
            } else {
                finalX = currentBuffer[targetIdx * 3]
                finalY = currentBuffer[targetIdx * 3 + 1]
            }

            val mapped = mapCoordsFast(finalX, finalY, latestSensorW, latestSensorH, latestRotation, screenW, screenH)
            
            handsData.add(
                HandData(
                    isLeft = isLeft,
                    joints = joints,
                    gesture = gesture,
                    pinchStrength = pinchVal,
                    x = mapped.first,
                    y = mapped.second
                )
            )
        }

        val primaryHand = handsData.first()
        _handState.value = HandState(
            isDetected = true,
            x = primaryHand.x,
            y = primaryHand.y,
            gesture = primaryHand.gesture,
            pinchStrength = primaryHand.pinchStrength,
            hands = handsData
        )
    }

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