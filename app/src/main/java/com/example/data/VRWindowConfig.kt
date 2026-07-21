package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class WindowContentType {
    COMPOSE_INTERNAL,
    ANDROID_APP,
    MEDIA_PLAYER
}

@Entity(tableName = "vr_windows")
data class VRWindowConfig(
    @PrimaryKey val id: String,
    val title: String,
    val worldX: Float = 0f,
    val worldY: Float = 0f,
    val worldZ: Float = 1.8f,
    val yaw: Float = 0f,
    val pitch: Float = 0f,
    val widthMeters: Float = 0.8f,
    val heightMeters: Float = 0.6f,
    val isOpen: Boolean = false,
    val isPinned: Boolean = false,
    val zIndex: Int = 0,
    val contentType: WindowContentType = WindowContentType.COMPOSE_INTERNAL,
    val appPackage: String? = null,
    val internalId: String = "",
    val brightness: Float = 1.0f,
    val contrast: Float = 1.0f
)