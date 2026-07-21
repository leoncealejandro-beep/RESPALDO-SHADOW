package com.example.spatial

import com.example.data.VRWindowConfig
import com.example.tracking.SensorFusion
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object SpatialRenderer {
    private const val DEFAULT_FOV_DEGREES = 70f
    private const val NEAR_PLANE = 0.1f
    private const val FAR_PLANE = 100f
    
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
            return if (len > 0f) Vec3(x / len, y / len, z / len) else Vec3(0f, 0f, 0f)
        }
    }
    
    data class Vec2(val x: Float, val y: Float)
    
    data class Matrix4(val data: FloatArray) {
        companion object {
            fun identity() = Matrix4(floatArrayOf(
                1f, 0f, 0f, 0f,
                0f, 1f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            ))
            
            fun perspective(fovDegrees: Float, aspect: Float, near: Float, far: Float): Matrix4 {
                val fovRad = Math.toRadians(fovDegrees.toDouble()).toFloat()
                val tanHalfFov = tan(fovRad / 2f)
                
                return Matrix4(floatArrayOf(
                    1f / (aspect * tanHalfFov), 0f, 0f, 0f,
                    0f, 1f / tanHalfFov, 0f, 0f,
                    0f, 0f, -(far + near) / (far - near), -(2f * far * near) / (far - near),
                    0f, 0f, -1f, 0f
                ))
            }
            
            fun rotationY(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    c, 0f, s, 0f,
                    0f, 1f, 0f, 0f,
                    -s, 0f, c, 0f,
                    0f, 0f, 0f, 1f
                ))
            }
            
            fun rotationX(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    1f, 0f, 0f, 0f,
                    0f, c, -s, 0f,
                    0f, s, c, 0f,
                    0f, 0f, 0f, 1f
                ))
            }
            
            fun rotationZ(angleDegrees: Float): Matrix4 {
                val rad = Math.toRadians(angleDegrees.toDouble()).toFloat()
                val c = cos(rad)
                val s = sin(rad)
                return Matrix4(floatArrayOf(
                    c, -s, 0f, 0f,
                    s, c, 0f, 0f,
                    0f, 0f, 1f, 0f,
                    0f, 0f, 0f, 1f
                ))
            }
        }
        
        fun multiply(other: Matrix4): Matrix4 {
            val result = FloatArray(16)
            for (row in 0 until 4) {
                for (col in 0 until 4) {
                    var sum = 0f
                    for (k in 0 until 4) {
                        sum += data[row * 4 + k] * other.data[k * 4 + col]
                    }
                    result[row * 4 + col] = sum
                }
            }
            return Matrix4(result)
        }
        
        fun transformPoint(point: Vec3): Vec3 {
            val w = data[12] * point.x + data[13] * point.y + data[14] * point.z + data[15]
            return Vec3(
                (data[0] * point.x + data[1] * point.y + data[2] * point.z + data[3]) / w,
                (data[4] * point.x + data[5] * point.y + data[6] * point.z + data[7]) / w,
                (data[8] * point.x + data[9] * point.y + data[10] * point.z + data[11]) / w
            )
        }
        
        fun transformDirection(direction: Vec3): Vec3 {
            return Vec3(
                data[0] * direction.x + data[1] * direction.y + data[2] * direction.z,
                data[4] * direction.x + data[5] * direction.y + data[6] * direction.z,
                data[8] * direction.x + data[9] * direction.y + data[10] * direction.z
            )
        }
    }
    
    fun getViewMatrix(orientation: SensorFusion.OrientationData): Matrix4 {
        val rotY = Matrix4.rotationY(-orientation.yaw)
        val rotX = Matrix4.rotationX(-orientation.pitch)
        val rotZ = Matrix4.rotationZ(-orientation.roll)
        return rotY.multiply(rotX).multiply(rotZ)
    }
    
    fun getProjectionMatrix(screenWidth: Int, screenHeight: Int, fovDegrees: Float = DEFAULT_FOV_DEGREES): Matrix4 {
        val aspect = screenWidth.toFloat() / screenHeight.toFloat()
        return Matrix4.perspective(fovDegrees, aspect, NEAR_PLANE, FAR_PLANE)
    }
    
    fun getVPMatrix(orientation: SensorFusion.OrientationData, screenWidth: Int, screenHeight: Int): Matrix4 {
        val view = getViewMatrix(orientation)
        val proj = getProjectionMatrix(screenWidth, screenHeight)
        return proj.multiply(view)
    }
    
    fun projectWorldToScreen(worldPos: Vec3, orientation: SensorFusion.OrientationData, screenWidth: Int, screenHeight: Int): Vec2 {
        val vpMatrix = getVPMatrix(orientation, screenWidth, screenHeight)
        val clipPos = vpMatrix.transformPoint(worldPos)
        
        val ndcX = clipPos.x
        val ndcY = clipPos.y
        
        val screenX = (ndcX + 1f) / 2f * screenWidth
        val screenY = (1f - ndcY) / 2f * screenHeight
        
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
            val invRotY = Matrix4.rotationY(window.yaw)
            val invRotX = Matrix4.rotationX(window.pitch)
            val windowToWorld = invRotY.multiply(invRotX)
            
            val worldNormal = windowToWorld.transformDirection(windowNormal).normalized()
            
            val denom = rayDir.dot(worldNormal)
            if (abs(denom) < 0.0001f) continue
            
            val t = (windowPos - rayOrigin).dot(worldNormal) / denom
            if (t < NEAR_PLANE || t > FAR_PLANE) continue
            
            val hitPoint = rayOrigin + (rayDir * t)
            val localPoint = windowToWorld.transformPoint(hitPoint - windowPos)
            
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
        val baseDistance = 2.0f
        val angleSpread = 30f * (PI.toFloat() / 180f)
        val windowCount = existingWindows.size
        
        val angle = if (windowCount % 2 == 0) {
            angleSpread / 2f
        } else {
            0f
        }
        
        val adjustedAngle = angle + Math.toRadians(headYaw.toDouble()).toFloat()
        val x = sin(adjustedAngle) * baseDistance
        val z = cos(adjustedAngle) * baseDistance
        val y = 0f
        
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