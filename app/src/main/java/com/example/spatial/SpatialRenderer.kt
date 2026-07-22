package com.example.spatial

import com.example.data.VRWindowConfig
import com.example.tracking.SensorFusion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object SpatialRenderer {
    // Constantes para VR Real
    private const val DEFAULT_FOV_DEGREES = 90f
    private const val NEAR_PLANE = 0.1f
    private const val FAR_PLANE = 100f
    private const val DEFAULT_IPD_METERS = 0.064f // 64mm estándar

    enum class Eye { LEFT, RIGHT }

    data class RaycastResult(
        val windowId: String,
        val worldPosition: Vec3,
        val screenUV: Vec2,
        val distance: Float
    )

    data class Vec3(val x: Float, val y: Float, val z: Float) {
        operator fun plus(other: Vec3) = Vec3(x + other.x, y + other.y, z + other.z)
        operator fun minus(other: Vec3) = Vec3(x - other.x, y - other.y, z - other.z)
        operator fun times(scalar: Float) = Vec3(x * scalar, y * scalar, z * scalar)
        fun dot(other: Vec3) = x * other.x + y * other.y + z * other.z
        fun cross(other: Vec3) = Vec3(
            y * other.z - z * other.y,
            z * other.x - x * other.z,
            x * other.y - y * other.x
        )
        fun length() = sqrt(x * x + y * y + z * z)
        fun normalized(): Vec3 {
            val len = length()
            return if (len > 1e-6f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 0f)
        }
    }

    data class Vec2(val x: Float, val y: Float)

    // Matrix4 optimizada para OpenGL ES (Column-Major Order)
    data class Matrix4(val data: FloatArray) {
        companion object {
            fun identity() = Matrix4(floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            ))

            // Proyección Perspectiva Asimétrica (Off-Axis) para soportar IPD real
            fun perspectiveOffAxis(
                fovYDegrees: Float, 
                aspect: Float, 
                near: Float, 
                far: Float, 
                eyeOffsetX: Float
            ): Matrix4 {
                val fovRad = Math.toRadians(fovYDegrees.toDouble()).toFloat()
                val top = near * tan(fovRad / 2f)
                val bottom = -top
                val right = top * aspect
                val left = -right

                // Desplazamiento del frustum para IPD
                val l = left + eyeOffsetX
                val r = right + eyeOffsetX
                val b = bottom
                val t = top

                return Matrix4(floatArrayOf(
                    2f * near / (r - l), 0f, (r + l) / (r - l), 0f,
                    0f, 2f * near / (t - b), (t + b) / (t - b), 0f,
                    0f, 0f, -(far + near) / (far - near), -(2f * far * near) / (far - near),
                    0f, 0f, -1f, 0f
                ))
            }

            fun translation(x: Float, y: Float, z: Float): Matrix4 {
                return Matrix4(floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, 1f, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    x, y, z, 1f
                ))
            }

            fun rotationY(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    c, 0f, -s, 0f,
                    0f, 1f, 0f, 0f,
                    s, 0f, c, 0f,
                    0f, 0f, 0f, 1f
                ))
            }

            fun rotationX(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, s, 0f,
                    0f, -s, c, 0f,
                    0f, 0f, 0f, 1f
                ))
            }

            fun rotationZ(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    c, s, 0f, 0f,
                    -s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                ))
            }
        }

        fun multiply(other: Matrix4): Matrix4 {
            val result = FloatArray(16)
            // Multiplicación Column-Major
            for (col in 0 until 4) {
                for (row in 0 until 4) {
                    var sum = 0f
                    for (k in 0 until 4) {
                        sum += data[k * 4 + row] * other.data[col * 4 + k]
                    }
                    result[col * 4 + row] = sum
                }
            }
            return Matrix4(result)
        }

        fun transformPoint(point: Vec3): Vec3 {
            val x = data[0] * point.x + data[4] * point.y + data[8] * point.z + data[12]
            val y = data[1] * point.x + data[5] * point.y + data[9] * point.z + data[13]
            val z = data[2] * point.x + data[6] * point.y + data[10] * point.z + data[14]
            val w = data[3] * point.x + data[7] * point.y + data[11] * point.z + data[15]
            
            return if (abs(w) > 1e-6f) Vec3(x / w, y / w, z / w) else Vec3(x, y, z)
        }

        fun transformDirection(direction: Vec3): Vec3 {
            return Vec3(
                data[0] * direction.x + data[4] * direction.y + data[8] * direction.z,
                data[1] * direction.x + data[5] * direction.y + data[9] * direction.z,
                data[2] * direction.x + data[6] * direction.y + data[10] * direction.z
            )
        }
        
        fun toFloatArray(): FloatArray = data
    }

    // --- API Pública ---

    fun getViewMatrix(orientation: SensorFusion.OrientationData): Matrix4 {
        val rotZ = Matrix4.rotationZ(-orientation.roll)
        val rotX = Matrix4.rotationX(-orientation.pitch)
        val rotY = Matrix4.rotationY(-orientation.yaw)
        return rotY.multiply(rotX).multiply(rotZ)
    }

    fun getEyeViewMatrix(eye: Eye, orientation: SensorFusion.OrientationData, ipd: Float = DEFAULT_IPD_METERS): Matrix4 {
        val eyeOffset = if (eye == Eye.LEFT) -ipd / 2f else ipd / 2f
        val eyeTranslation = Matrix4.translation(eyeOffset, 0f, 0f)
        val headView = getViewMatrix(orientation)
        return eyeTranslation.multiply(headView)
    }

    fun getProjectionMatrix(screenWidth: Int, screenHeight: Int, fovDegrees: Float = DEFAULT_FOV_DEGREES): Matrix4 {
        val aspect = screenWidth.toFloat() / screenHeight.toFloat()
        return Matrix4.perspectiveOffAxis(fovDegrees, aspect, NEAR_PLANE, FAR_PLANE, 0f)
    }

    fun getEyeProjectionMatrix(eye: Eye, screenWidth: Int, screenHeight: Int, fovDegrees: Float = DEFAULT_FOV_DEGREES, ipd: Float = DEFAULT_IPD_METERS): Matrix4 {
        val aspect = screenWidth.toFloat() / screenHeight.toFloat()
        val eyeOffset = if (eye == Eye.LEFT) -ipd / 2f else ipd / 2f
        return Matrix4.perspectiveOffAxis(fovDegrees, aspect, NEAR_PLANE, FAR_PLANE, eyeOffset)
    }

    fun getVPMatrix(orientation: SensorFusion.OrientationData, screenWidth: Int, screenHeight: Int): Matrix4 {
        val view = getViewMatrix(orientation)
        val proj = getProjectionMatrix(screenWidth, screenHeight)
        return proj.multiply(view)
    }

    fun projectWorldToScreen(worldPos: Vec3, orientation: SensorFusion.OrientationData, screenWidth: Int, screenHeight: Int): Vec2 {
        val vpMatrix = getVPMatrix(orientation, screenWidth, screenHeight)
        val clipPos = vpMatrix.transformPoint(worldPos)
        
        val screenX = (clipPos.x + 1f) / 2f * screenWidth
        val screenY = (1f - clipPos.y) / 2f * screenHeight
        
        return Vec2(screenX, screenY)
    }

    fun screenToRay(screenX: Float, screenY: Float, screenWidth: Int, screenHeight: Int, orientation: SensorFusion.OrientationData): Pair<Vec3, Vec3> {
        val ndcX = (screenX / screenWidth) * 2f - 1f
        val ndcY = 1f - (screenY / screenHeight) * 2f
        
        val aspect = screenWidth.toFloat() / screenHeight.toFloat()
        val fovRad = Math.toRadians(DEFAULT_FOV_DEGREES.toDouble()).toFloat()
        val tanHalfFov = tan(fovRad / 2f)
        
        val viewX = ndcX * aspect * tanHalfFov
        val viewY = ndcY * tanHalfFov
        
        // OpenGL: -Z es forward
        val rayDirView = Vec3(viewX, viewY, -1f).normalized()
        
        val invView = Matrix4.rotationZ(orientation.roll)
            .multiply(Matrix4.rotationX(orientation.pitch))
            .multiply(Matrix4.rotationY(orientation.yaw))
        
        val rayDirWorld = invView.transformDirection(rayDirView).normalized()
        val rayOrigin = Vec3(0f, 0f, 0f)
        
        return Pair(rayOrigin, rayDirWorld)
    }

    fun raycastWindows(
        screenX: Float,
        screenY: Float,
        screenWidth: Int,
        screenHeight: Int,
        orientation: SensorFusion.OrientationData,
        windows: List<VRWindowConfig>
    ): RaycastResult? {
        val (rayOrigin, rayDir) = screenToRay(screenX, screenY, screenWidth, screenHeight, orientation)
        
        var closestHit: RaycastResult? = null
        var closestDistance = Float.MAX_VALUE
        
        for (window in windows) {
            if (!window.isOpen) continue
            
            val windowPos = Vec3(window.worldX, window.worldY, window.worldZ)
            val halfWidth = window.widthMeters / 2f
            val halfHeight = window.heightMeters / 2f
            
            val windowNormal = Vec3(0f, 0f, 1f)
            val rotY = Matrix4.rotationY(window.yaw)
            val rotX = Matrix4.rotationX(window.pitch)
            val windowToWorld = rotY.multiply(rotX)
            
            val worldNormal = windowToWorld.transformDirection(windowNormal).normalized()
            
            val denom = rayDir.dot(worldNormal)
            if (abs(denom) < 0.0001f) continue
            
            val t = (windowPos - rayOrigin).dot(worldNormal) / denom
            if (t < NEAR_PLANE || t > FAR_PLANE) continue
            
            val hitPoint = rayOrigin + (rayDir * t)
            
            // Transformar a espacio local
            val worldToWindow = Matrix4.rotationX(-window.pitch).multiply(Matrix4.rotationY(-window.yaw))
            val localPoint = worldToWindow.transformPoint(hitPoint - windowPos)
            
            if (abs(localPoint.x) <= halfWidth && abs(localPoint.y) <= halfHeight) {
                val uvX = (localPoint.x + halfWidth) / window.widthMeters
                val uvY = 1f - (localPoint.y + halfHeight) / window.heightMeters
                
                if (t < closestDistance) {
                    closestDistance = t
                    closestHit = RaycastResult(
                        windowId = window.id,
                        worldPosition = hitPoint,
                        screenUV = Vec2(uvX, uvY),
                        distance = t
                    )
                }
            }
        }
        
        return closestHit
    }

    fun computeAutoLayoutPosition(existingWindows: List<VRWindowConfig>, headYaw: Float): Triple<Float, Float, Float> {
        // Lógica para VR Real: Arco esférico frente al usuario
        val distance = 2.5f // Distancia cómoda de lectura/interacción
        val angleStep = 25f // Grados entre ventanas
        
        val windowCount = existingWindows.size
        val baseAngle = if (windowCount == 0) 0f else windowCount * angleStep
        
        // Calcular posición relativa al yaw de la cabeza (frente al usuario)
        val totalAngleRad = Math.toRadians((headYaw + baseAngle).toDouble()).toFloat()
        
        // Coordenadas esféricas a cartesianas (OpenGL: -Z is forward)
        val x = sin(totalAngleRad) * distance
        val z = -cos(totalAngleRad) * distance // Negativo para estar frente a la cámara
        val y = 0f // Altura de los ojos
        
        return Triple(x, y, z)
    }

    fun windowToScreenMatrix(
        window: VRWindowConfig,
        orientation: SensorFusion.OrientationData,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Vec2, Vec2> {
        val windowPos = Vec3(window.worldX, window.worldY, window.worldZ)
        val halfWidth = window.widthMeters / 2f
        val halfHeight = window.heightMeters / 2f
        
        val topLeft = Vec3(-halfWidth, halfHeight, 0f)
        val bottomRight = Vec3(halfWidth, -halfHeight, 0f)
        
        val rotY = Matrix4.rotationY(window.yaw)
        val rotX = Matrix4.rotationX(window.pitch)
        val windowToWorld = rotY.multiply(rotX)
        
        val topLeftWorld = windowToWorld.transformPoint(topLeft) + windowPos
        val bottomRightWorld = windowToWorld.transformPoint(bottomRight) + windowPos
        
        val topLeftScreen = projectWorldToScreen(topLeftWorld, orientation, screenWidth, screenHeight)
        val bottomRightScreen = projectWorldToScreen(bottomRightWorld, orientation, screenWidth, screenHeight)
        
        return Pair(topLeftScreen, bottomRightScreen)
    }

    fun calculateWindowScreenSize(
        window: VRWindowConfig,
        orientation: SensorFusion.OrientationData,
        screenWidth: Int,
        screenHeight: Int
    ): Pair<Float, Float> {
        val (topLeft, bottomRight) = windowToScreenMatrix(window, orientation, screenWidth, screenHeight)
        val width = abs(bottomRight.x - topLeft.x)
        val height = abs(bottomRight.y - topLeft.y)
        return Pair(width, height)
    }
}