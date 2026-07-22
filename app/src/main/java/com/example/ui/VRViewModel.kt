package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.VRRepository
import com.example.data.VRWindowConfig
import com.example.data.WindowContentType
import com.example.spatial.SpatialRenderer
import com.example.tracking.HandTracker
import com.example.tracking.SensorFusion
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI

data class LensConfig(
    val ipd: Float = 64f,
    val leftScaleX: Float = 1.0f,
    val leftScaleY: Float = 1.0f,
    val rightScaleX: Float = 1.0f,
    val rightScaleY: Float = 1.0f,
    val leftOffsetX: Float = 0f,
    val leftOffsetY: Float = 0f,
    val rightOffsetX: Float = 0f,
    val rightOffsetY: Float = 0f,
    val horizontalRotation: Float = 0f,
    val verticalRotation: Float = 0f,
    val distortionIntensity: Float = 1.0f
)

class VRViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val context = application.applicationContext
    private val database = AppDatabase.getDatabase(context)
    val repository = VRRepository(database.vrWindowDao())

    // ---------- Tracking ----------
    private val sensorFusion = SensorFusion(context)

    val headOrientation = sensorFusion.orientation.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SensorFusion.OrientationData(yaw = 0f, pitch = 0f, roll = 0f)
    )

    val handTracker = HandTracker(context)
    val handState = handTracker.handState

    // ---------- Window state ----------
    private val _windowConfigs = MutableStateFlow<Map<String, VRWindowConfig>>(emptyMap())
    val windowConfigs: StateFlow<Map<String, VRWindowConfig>> = _windowConfigs.asStateFlow()

    private val _focusedWindowId = MutableStateFlow<String?>(null)
    val focusedWindowId: StateFlow<String?> = _focusedWindowId.asStateFlow()

    private val _grabbedWindowId = MutableStateFlow<String?>(null)
    val grabbedWindowId: StateFlow<String?> = _grabbedWindowId.asStateFlow()

    // ========================================================================
    // MODO EDITOR
    // ========================================================================
    private val _isEditorMode = MutableStateFlow(false)
    val isEditorMode: StateFlow<Boolean> = _isEditorMode.asStateFlow()

    private val _selectedWindowId = MutableStateFlow<String?>(null)
    val selectedWindowId: StateFlow<String?> = _selectedWindowId.asStateFlow()

    // ---------- System settings ----------
    // CORRECCIÓN ARQUITECTÓNICA: LensConfig es ahora la única fuente de verdad.
    // Estos StateFlows son de solo lectura y se derivan directamente de _lensConfig 
    // para evitar divergencia de estados e incompatibilidades con VRConfigActivity.
    // ---------- Lens Configuration ----------
private val _lensConfig = MutableStateFlow(LensConfig())

val lensConfig: StateFlow<LensConfig> = _lensConfig.asStateFlow()

// ---------- System settings ----------
val ipd: StateFlow<Float> =
    _lensConfig
        .map { it.ipd }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            64f
        )

val lensCorrection: StateFlow<Float> =
    _lensConfig
        .map { it.distortionIntensity }
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            1.0f
        )

val backgroundCameraEnabled = MutableStateFlow(true)
val visualTheme = MutableStateFlow("glassmorphic")
val handStyle = MutableStateFlow("holographic")

    // ---------- Browser ----------
    val currentUrl = MutableStateFlow("https://www.google.com")
    val browserTabs = MutableStateFlow(listOf("https://www.google.com"))
    val activeTabIdx = MutableStateFlow(0)

    // ---------- File Explorer ----------
    data class VRFile(
        val name: String,
        val path: String,
        val size: String,
        val isDirectory: Boolean,
        val mimeType: String
    )

    val currentPath = MutableStateFlow("")
    val filesList = MutableStateFlow<List<VRFile>>(emptyList())

    // ---------- Gallery ----------
    val galleryImages = MutableStateFlow<List<String>>(emptyList())
    val selectedGalleryIdx = MutableStateFlow(-1)

    // ---------- Media ----------
    val mediaUrls = MutableStateFlow(
        listOf("https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4")
    )
    val activeMediaUrl = MutableStateFlow(mediaUrls.value.first())
    val isMediaPlaying = MutableStateFlow(false)

    // ---------- Keyboard ----------
    val keyboardInput = MutableStateFlow("")
    val keyboardTargetWindow = MutableStateFlow<String?>(null)
    val isShiftActive = MutableStateFlow(false)

    // ---------- Assistant ----------
    data class ChatMessage(
        val sender: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis()
    )

    val assistantMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(ChatMessage("assistant", "Hello! I'm your Shadow VR assistant."))
    )
    val assistantQuery = MutableStateFlow("")
    val isAssistantLoading = MutableStateFlow(false)

    // ---------- Notifications ----------
    data class VRNotification(
        val id: Int,
        val title: String,
        val body: String,
        val app: String
    )

    val notificationFeed = MutableStateFlow<List<VRNotification>>(emptyList())
    private var notifSeq = 0

    // ---------- App drawer ----------
    data class InstalledApp(val packageName: String, val label: String)

    private val _installedApps = MutableStateFlow<List<InstalledApp>>(emptyList())
    val installedApps: StateFlow<List<InstalledApp>> = _installedApps.asStateFlow()

    val isAppDrawerOpen = MutableStateFlow(false)
    val appDrawerSearchQuery = MutableStateFlow("")
    val favoriteApps = MutableStateFlow<Set<String>>(emptySet())
    val recentApps = MutableStateFlow<List<String>>(emptyList())
    val appDrawerCategory = MutableStateFlow("All")

    // ---------- TTS ----------
    private var tts: TextToSpeech? = null

    // Canonical internal ids
    private val internalApps = mapOf(
        "launcher" to "System Dashboard",
        "browser" to "Spatial Web Browser",
        "explorer" to "Spatial File Explorer",
        "gallery" to "Media Gallery",
        "media" to "Cinematic Player",
        "assistant" to "Gemini AI Helper",
        "settings" to "System Preferences",
        "keyboard" to "Spatial Keyboard",
        "universal_dock" to "Universal Dock"
    )

    init {
        tts = TextToSpeech(context, this)

        viewModelScope.launch {
            repository.windowConfigs.collect { configs ->
                if (configs.isEmpty()) {
                    val defaults = buildDefaultWindows()
                    repository.saveWindowConfigs(defaults)
                } else {
                    _windowConfigs.value = configs.associateBy { it.id }
                }
            }
        }

        setupVirtualFileSystem()
        loadInstalledApps()
        
        // CORRECCIÓN: Carga síncrona para evitar condiciones de carrera al iniciar la UI
        loadLensConfiguration()
        
        addNotification("System", "Welcome to Shadow VR. Look around and pinch to interact.", "System")

        viewModelScope.launch {
            delay(800)
            recenterWorld()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) tts?.language = Locale.getDefault()
    }

    fun speak(text: String) {
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    // ---------- Tracking lifecycle ----------
    fun startTracking() {
        sensorFusion.start()
    }

    fun stopTracking() {
        sensorFusion.stop()
    }

    override fun onCleared() {
        sensorFusion.stop()
        tts?.shutdown()
        super.onCleared()
    }

    // ========================================================================
    // LENS CONFIGURATION SYSTEM (API Clara para VRConfigActivity)
    // ========================================================================

    fun updateIPD(newIpd: Float) {
        val clampedIpd = newIpd.coerceIn(45f, 75f)
        // CORRECCIÓN: Única fuente de verdad es _lensConfig
        _lensConfig.update { it.copy(ipd = clampedIpd) }
    }

    fun updateLeftLens(scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
        _lensConfig.update { 
            it.copy(
                leftScaleX = scaleX,
                leftScaleY = scaleY,
                leftOffsetX = offsetX,
                leftOffsetY = offsetY
            )
        }
    }

    fun updateRightLens(scaleX: Float, scaleY: Float, offsetX: Float, offsetY: Float) {
        _lensConfig.update { 
            it.copy(
                rightScaleX = scaleX,
                rightScaleY = scaleY,
                rightOffsetX = offsetX,
                rightOffsetY = offsetY
            )
        }
    }

    fun updateLensRotation(horizontal: Float, vertical: Float) {
        _lensConfig.update {
            it.copy(
                horizontalRotation = horizontal,
                verticalRotation = vertical
            )
        }
    }

    fun updateDistortion(intensity: Float) {
        val clamped = intensity.coerceIn(0.5f, 2.0f)
        // CORRECCIÓN: Única fuente de verdad es _lensConfig
        _lensConfig.update { it.copy(distortionIntensity = clamped) }
    }

    fun resetLensConfiguration() {
        val default = LensConfig()
        _lensConfig.value = default
        
        val prefs = context.getSharedPreferences("shadow_vr_lens_prefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }

fun setLensCorrection(value: Float) {
    _lensConfig.update { current ->
        current.copy(
            distortionIntensity = value.coerceIn(0.5f, 2.0f)
        )
    }
}


    // CORRECCIÓN: Ejecución síncrona con apply() para garantizar persistencia 
    // incluso si VRConfigActivity se destruye inmediatamente después de llamar a este método.
    fun saveLensConfiguration() {
        val prefs = context.getSharedPreferences("shadow_vr_lens_prefs", Context.MODE_PRIVATE)
        val config = _lensConfig.value
        prefs.edit().apply {
            putFloat("lens_ipd", config.ipd)
            putFloat("lens_left_scale_x", config.leftScaleX)
            putFloat("lens_left_scale_y", config.leftScaleY)
            putFloat("lens_right_scale_x", config.rightScaleX)
            putFloat("lens_right_scale_y", config.rightScaleY)
            putFloat("lens_left_offset_x", config.leftOffsetX)
            putFloat("lens_left_offset_y", config.leftOffsetY)
            putFloat("lens_right_offset_x", config.rightOffsetX)
            putFloat("lens_right_offset_y", config.rightOffsetY)
            putFloat("lens_horizontal_rotation", config.horizontalRotation)
            putFloat("lens_vertical_rotation", config.verticalRotation)
            putFloat("lens_distortion_intensity", config.distortionIntensity)
            apply()
        }
    }

    // CORRECCIÓN: Carga síncrona para asegurar que la configuración esté disponible 
    // antes de que cualquier componente de UI intente leerla.
    private fun loadLensConfiguration() {
        val prefs = context.getSharedPreferences("shadow_vr_lens_prefs", Context.MODE_PRIVATE)
        val config = LensConfig(
            ipd = prefs.getFloat("lens_ipd", 64f).coerceIn(45f, 75f),
            leftScaleX = prefs.getFloat("lens_left_scale_x", 1.0f),
            leftScaleY = prefs.getFloat("lens_left_scale_y", 1.0f),
            rightScaleX = prefs.getFloat("lens_right_scale_x", 1.0f),
            rightScaleY = prefs.getFloat("lens_right_scale_y", 1.0f),
            leftOffsetX = prefs.getFloat("lens_left_offset_x", 0f),
            leftOffsetY = prefs.getFloat("lens_left_offset_y", 0f),
            rightOffsetX = prefs.getFloat("lens_right_offset_x", 0f),
            rightOffsetY = prefs.getFloat("lens_right_offset_y", 0f),
            horizontalRotation = prefs.getFloat("lens_horizontal_rotation", 0f),
            verticalRotation = prefs.getFloat("lens_vertical_rotation", 0f),
            distortionIntensity = prefs.getFloat("lens_distortion_intensity", 1.0f).coerceIn(0.5f, 2.0f)
        )
        _lensConfig.value = config
    }

    // ========================================================================
    // LÓGICA DEL MODO EDITOR
    // ========================================================================

    fun setEditorMode(enabled: Boolean) {
        _isEditorMode.value = enabled
        if (!enabled) {
            _selectedWindowId.value = null
        }
    }

    fun selectWindow(id: String?) {
        _selectedWindowId.value = id
    }

    fun addNewWindow() {
        viewModelScope.launch {
            val newId = "win_${System.currentTimeMillis()}"
            val count = _windowConfigs.value.size
            
            val (x, y, z) = SpatialRenderer.computeAutoLayoutPosition(
                _windowConfigs.value.values.toList(), 
                headOrientation.value.yaw
            )

            val newWin = VRWindowConfig(
                id = newId,
                title = "Nueva Ventana ${count + 1}",
                worldX = x,
                worldY = y,
                worldZ = z.coerceIn(1.5f, 3.0f),
                widthMeters = 0.8f,
                heightMeters = 0.6f,
                isOpen = true,
                contentType = WindowContentType.COMPOSE_INTERNAL,
                internalId = newId
            )
            repository.saveWindowConfig(newWin)
            _selectedWindowId.value = newId
            speak("Window added")
        }
    }

    fun deleteSelectedWindow() {
        val id = _selectedWindowId.value ?: return
        if (id == "universal_dock") return
        
        viewModelScope.launch {
            repository.deleteWindowConfig(id) 
            _selectedWindowId.value = null
            speak("Window deleted")
        }
    }

    fun updateSelectedWindow(block: VRWindowConfig.() -> VRWindowConfig) {
        val id = _selectedWindowId.value ?: return
        val current = _windowConfigs.value[id] ?: return
        
        viewModelScope.launch {
            val updated = current.block()
            repository.saveWindowConfig(updated)
        }
    }

    // ---------- Default windows ----------
    private fun buildDefaultWindows(): List<VRWindowConfig> = listOf(
        VRWindowConfig(
            id = "universal_dock",
            title = "Universal Dock",
            worldX = 0f,
            worldY = -0.8f,
            worldZ = 1.5f,
            pitch = 0f,
            widthMeters = 0.85f,
            heightMeters = 0.15f,
            isOpen = true,
            isPinned = true,
            zIndex = 200,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "universal_dock"
        ),
        VRWindowConfig(
            id = "launcher", title = "System Dashboard",
            worldX = 0f, worldY = 0.1f, worldZ = 1.6f,
            pitch = 0f,
            widthMeters = 0.9f, heightMeters = 0.6f,
            isOpen = true, isPinned = false, zIndex = 100,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "launcher"
        ),
        VRWindowConfig(
            id = "browser", title = "Spatial Web Browser",
            worldX = -0.9f, worldY = 0f, worldZ = 1.8f,
            pitch = 0f,
            widthMeters = 1.0f, heightMeters = 0.7f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "browser"
        ),
        VRWindowConfig(
            id = "explorer", title = "File Explorer",
            worldX = 0.9f, worldY = 0f, worldZ = 1.8f,
            pitch = 0f,
            widthMeters = 0.9f, heightMeters = 0.65f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "explorer"
        ),
        VRWindowConfig(
            id = "gallery", title = "Media Gallery",
            worldX = -1.3f, worldY = -0.2f, worldZ = 2.0f,
            pitch = 0f,
            widthMeters = 0.9f, heightMeters = 0.6f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "gallery"
        ),
        VRWindowConfig(
            id = "media", title = "Cinematic Player",
            worldX = 0f, worldY = 0.3f, worldZ = 2.2f,
            pitch = 0f,
            widthMeters = 1.4f, heightMeters = 0.8f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "media"
        ),
        VRWindowConfig(
            id = "assistant", title = "Gemini AI Helper",
            worldX = 1.3f, worldY = -0.2f, worldZ = 1.8f,
            pitch = 0f,
            widthMeters = 0.7f, heightMeters = 0.8f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "assistant"
        ),
        VRWindowConfig(
            id = "settings", title = "System Preferences",
            worldX = 0f, worldY = -0.4f, worldZ = 1.6f,
            pitch = 0f,
            widthMeters = 0.8f, heightMeters = 0.6f,
            isOpen = false, isPinned = false, zIndex = 10,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "settings"
        ),
        VRWindowConfig(
            id = "keyboard", title = "Spatial Keyboard",
            worldX = 0f, worldY = -0.5f, worldZ = 1.2f,
            pitch = 0f,
            widthMeters = 0.9f, heightMeters = 0.35f,
            isOpen = false, isPinned = false, zIndex = 50,
            contentType = WindowContentType.COMPOSE_INTERNAL,
            internalId = "keyboard"
        )
    )

    // ---------- Window management ----------
    private fun findByInternalId(internalId: String): VRWindowConfig? =
        _windowConfigs.value.values.firstOrNull { it.internalId == internalId }

    fun openInternalWindow(internalId: String) {
        viewModelScope.launch {
            val existing = findByInternalId(internalId)
            if (existing == null) {
                val (x, y, z) = SpatialRenderer.computeAutoLayoutPosition(
                    _windowConfigs.value.values.toList(), headOrientation.value.yaw
                )
                val title = internalApps[internalId] ?: internalId
                val newConfig = VRWindowConfig(
                    id = internalId, title = title,
                    worldX = x, worldY = y, worldZ = z,
                    widthMeters = 0.9f, heightMeters = 0.65f,
                    isOpen = true,
                    contentType = WindowContentType.COMPOSE_INTERNAL,
                    internalId = internalId
                )
                repository.saveWindowConfig(newConfig)
            } else if (!existing.isOpen) {
                repository.saveWindowConfig(existing.copy(isOpen = true))
            }
            _focusedWindowId.value = internalId
        }
    }

    fun openExternalApp(packageName: String) {
        val windowId = "app_$packageName"
        viewModelScope.launch {
            val existing = _windowConfigs.value[windowId]
            val config = existing ?: run {
                val label = try {
                    val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
                    context.packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: Exception) {
                    packageName
                }
                val (x, y, z) = SpatialRenderer.computeAutoLayoutPosition(
                    _windowConfigs.value.values.toList(), headOrientation.value.yaw
                )
                VRWindowConfig(
                    id = windowId, title = label,
                    worldX = x, worldY = y, worldZ = z,
                    widthMeters = 1.0f, heightMeters = 0.7f,
                    isOpen = true,
                    contentType = WindowContentType.ANDROID_APP,
                    appPackage = packageName,
                    internalId = "external_app"
                )
            }
            repository.saveWindowConfig(config)
            _focusedWindowId.value = windowId
            addToRecentApps(packageName)
            try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return@launch
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                speak("Opening ${config.title}")
            } catch (e: Exception) {
                Toast.makeText(context, "Cannot launch: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun closeWindow(id: String) {
        if (id == "universal_dock") return
        viewModelScope.launch {
            val cfg = findByInternalId(id) ?: _windowConfigs.value[id]
            if (cfg != null) {
                repository.saveWindowConfig(cfg.copy(isOpen = false))
            }
            if (_focusedWindowId.value == id || cfg?.id == _focusedWindowId.value) {
                _focusedWindowId.value = null
            }
        }
    }

    fun toggleWindow(id: String, open: Boolean) {
        if (id == "universal_dock") return
        viewModelScope.launch {
            val cfg = findByInternalId(id) ?: _windowConfigs.value[id]
            if (cfg != null) {
                repository.saveWindowConfig(cfg.copy(isOpen = open))
                if (open) _focusedWindowId.value = cfg.id
            }
        }
    }

    fun moveWindowToWorld(id: String, x: Float, y: Float, z: Float) {
        viewModelScope.launch {
            val cfg = findByInternalId(id) ?: _windowConfigs.value[id]
            if (cfg != null) {
                repository.saveWindowConfig(cfg.copy(worldX = x, worldY = y, worldZ = z))
            }
        }
    }

    fun resizeWindow(id: String, widthMeters: Float, heightMeters: Float) {
        viewModelScope.launch {
            val cfg = findByInternalId(id) ?: _windowConfigs.value[id]
            if (cfg != null) {
                repository.saveWindowConfig(
                    cfg.copy(
                        widthMeters = widthMeters.coerceIn(0.4f, 3.0f),
                        heightMeters = heightMeters.coerceIn(0.3f, 2.0f)
                    )
                )
            }
        }
    }

    fun startGrab(windowId: String) {
        if (windowId == "universal_dock") return
        _grabbedWindowId.value = windowId
        _focusedWindowId.value = windowId
    }

    fun endGrab() {
        _grabbedWindowId.value = null
    }

    // ---------- Workspace ----------
    fun recenterWorld() {
        val headYaw = headOrientation.value.yaw
        viewModelScope.launch {
            val openWindows = _windowConfigs.value.values.filter { it.isOpen }
            if (openWindows.isEmpty()) {
                speak("Workspace recentered")
                return@launch
            }
            val total = openWindows.size
            val spread = 45f * (PI.toFloat() / 180f)
            val step = if (total > 1) (2 * spread) / (total - 1) else 0f
            val updated = openWindows.mapIndexed { index, win ->
                val angle = -spread + index * step + headYaw
                val distance = win.worldZ.coerceIn(1.4f, 2.5f)
                val x = kotlin.math.sin(angle) * distance
                val z = kotlin.math.cos(angle) * distance
                win.copy(worldX = x, worldY = win.worldY, worldZ = z)
            }
            repository.saveWindowConfigs(updated)
            speak("Workspace recentered")
        }
    }

    fun resetLayout() {
        viewModelScope.launch {
            val defaults = buildDefaultWindows()
            repository.saveWindowConfigs(defaults)
            _focusedWindowId.value = "launcher"
            sensorFusion.reset()
            delay(100)
            recenterWorld()
        }
    }

    // ---------- Browser ----------
    fun navigateBrowser(url: String) {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return
        val formatted = if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
            "https://$trimmed"
        } else trimmed
        currentUrl.value = formatted
        browserTabs.update { tabs ->
            val idx = activeTabIdx.value
            val mutable = tabs.toMutableList()
            if (idx in mutable.indices) {
                mutable[idx] = formatted
            } else if (mutable.isNotEmpty()) {
                mutable[mutable.lastIndex] = formatted
            } else {
                mutable.add(formatted)
            }
            mutable
        }
    }

    fun addBrowserTab() {
        browserTabs.update { it + "https://www.google.com" }
        activeTabIdx.value = browserTabs.value.lastIndex
        currentUrl.value = "https://www.google.com"
    }

    fun closeBrowserTab(index: Int) {
        browserTabs.update { tabs ->
            if (tabs.isEmpty()) return@update tabs
            val mutable = tabs.toMutableList()
            if (index in mutable.indices) mutable.removeAt(index)
            if (mutable.isEmpty()) mutable.add("https://www.google.com")
            mutable
        }
        val newIdx = activeTabIdx.value
            .coerceAtMost(browserTabs.value.lastIndex)
            .coerceAtLeast(0)
        activeTabIdx.value = newIdx
        currentUrl.value = browserTabs.value.getOrElse(newIdx) { "https://www.google.com" }
    }

    fun selectBrowserTab(index: Int) {
        if (index !in browserTabs.value.indices) return
        activeTabIdx.value = index
        currentUrl.value = browserTabs.value[index]
    }

    // ---------- File system ----------
    private fun setupVirtualFileSystem() {
        val root = java.io.File(context.filesDir, "spatial_root")
        if (!root.exists()) {
            root.mkdirs()
            java.io.File(root, "Photos").mkdirs()
            java.io.File(root, "Movies").mkdirs()
            java.io.File(root, "Notes").mkdirs()
            java.io.File(root, "Notes/welcome.txt").writeText("Welcome to Shadow VR.")
        }
        navigateToDirectory(root.absolutePath)
        galleryImages.value = listOf(
            "https://picsum.photos/id/10/800/600",
            "https://picsum.photos/id/11/800/600",
            "https://picsum.photos/id/12/800/600"
        )
    }

    fun navigateToDirectory(path: String) {
        val folder = java.io.File(path)
        if (!folder.exists() || !folder.isDirectory) return
        currentPath.value = path
        val items = folder.listFiles() ?: emptyArray()
        filesList.value = items.map {
            VRFile(
                name = it.name,
                path = it.absolutePath,
                size = if (it.isDirectory) "${it.listFiles()?.size ?: 0} items" else "${it.length() / 1024} KB",
                isDirectory = it.isDirectory,
                mimeType = if (it.isDirectory) "folder" else "text/plain"
            )
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name }))
    }

    fun upOneFolder() {
        val current = java.io.File(currentPath.value)
        val rootPath = java.io.File(context.filesDir, "spatial_root").absolutePath
        current.parentFile?.let {
            if (current.absolutePath != rootPath) navigateToDirectory(it.absolutePath)
        }
    }

    fun deleteFile(path: String) {
        java.io.File(path).deleteRecursively()
        navigateToDirectory(currentPath.value)
    }

    fun createFolder(name: String) {
        val current = java.io.File(currentPath.value)
        val newFolder = java.io.File(current, name)
        if (!newFolder.exists()) {
            newFolder.mkdirs()
            navigateToDirectory(currentPath.value)
        }
    }

    fun createFile(name: String, content: String) {
        val current = java.io.File(currentPath.value)
        val newFile = java.io.File(current, name)
        if (!newFile.exists()) {
            newFile.writeText(content)
            navigateToDirectory(currentPath.value)
        }
    }

    // ---------- Keyboard ----------
    fun focusKeyboardFor(windowId: String) {
        keyboardTargetWindow.value = windowId
        keyboardInput.value = when (windowId) {
            "browser" -> currentUrl.value
            "assistant" -> assistantQuery.value
            "explorer" -> ""
            else -> ""
        }
        toggleWindow("keyboard", true)
        _focusedWindowId.value = windowId
    }

    fun setKeyboardInput(value: String) {
        keyboardInput.value = value
    }

    fun toggleShift() {
        isShiftActive.update { !it }
    }

    fun appendKey(rawChar: String) {
        when (rawChar) {
            "⌫" -> keyboardInput.update { if (it.isNotEmpty()) it.dropLast(1) else it }
            "Space" -> keyboardInput.update { it + " " }
            "Shift" -> toggleShift()
            "Enter" -> commitKeyboardInput()
            else -> {
                val ch = if (isShiftActive.value) rawChar.uppercase(Locale.ROOT) else rawChar.lowercase(Locale.ROOT)
                keyboardInput.update { it + ch }
                if (isShiftActive.value) isShiftActive.value = false
            }
        }
    }

    private fun commitKeyboardInput() {
        val input = keyboardInput.value
        when (keyboardTargetWindow.value) {
            "browser" -> navigateBrowser(input)
            "assistant" -> {
                assistantQuery.value = input
                sendAssistantMessage()
            }
            "explorer" -> { }
            "app_drawer" -> appDrawerSearchQuery.value = input
        }
        keyboardInput.value = ""
        toggleWindow("keyboard", false)
    }

    // ---------- Assistant ----------
    fun sendAssistantMessage() {
        val query = assistantQuery.value.trim()
        if (query.isEmpty()) return
        assistantMessages.update { it + ChatMessage("user", query) }
        assistantQuery.value = ""
        isAssistantLoading.value = true
        viewModelScope.launch {
            val lower = query.lowercase(Locale.ROOT)
            when {
                lower.contains("open browser") -> openInternalWindow("browser")
                lower.contains("open files") || lower.contains("open explorer") -> openInternalWindow("explorer")
                lower.contains("close settings") -> closeWindow("settings")
                lower.contains("recenter") || lower.contains("reset") -> recenterWorld()
            }
            delay(400)
            assistantMessages.update { it + ChatMessage("assistant", "Done. I've processed: $query") }
            isAssistantLoading.value = false
        }
    }

    // ---------- Notifications ----------
    fun addNotification(title: String, body: String, app: String) {
        notificationFeed.update { list ->
            val mutable = list.toMutableList()
            mutable.add(0, VRNotification(notifSeq++, title, body, app))
            if (mutable.size > 5) mutable.removeAt(mutable.size - 1)
            mutable
        }
    }

    fun dismissNotification(id: Int) {
        notificationFeed.update { it.filter { n -> n.id != id } }
    }

    // ---------- Installed apps ----------
    fun loadInstalledApps() {
        viewModelScope.launch {
            try {
                val intent = Intent(Intent.ACTION_MAIN, null).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                val infos = context.packageManager.queryIntentActivities(intent, 0)
                _installedApps.value = infos.map {
                    InstalledApp(
                        it.activityInfo.packageName,
                        it.loadLabel(context.packageManager).toString()
                    )
                }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun toggleFavoriteApp(pkg: String) {
        favoriteApps.update { cur -> if (cur.contains(pkg)) cur - pkg else cur + pkg }
    }

    fun addToRecentApps(pkg: String) {
        recentApps.update { cur ->
            val mutable = cur.toMutableList()
            mutable.remove(pkg)
            mutable.add(0, pkg)
            if (mutable.size > 12) mutable.removeAt(mutable.size - 1)
            mutable
        }
    }
}