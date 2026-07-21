package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// --- ENTITIES ---

// NOTA: VRWindowConfig ha sido movido a su propio archivo VRWindowConfig.kt 
// para evitar conflictos de redeclaración y usar la estructura espacial completa.

@Entity(tableName = "vr_settings")
data class VRSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "browser_bookmarks")
data class BrowserBookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String
)

@Entity(tableName = "browser_history")
data class BrowserHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO ---



// --- DATABASE ---



// --- REPOSITORY ---

