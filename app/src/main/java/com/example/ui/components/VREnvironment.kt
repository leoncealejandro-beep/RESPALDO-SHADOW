package com.example.ui.components

import android.media.AudioManager
import android.media.ToneGenerator
import android.view.MotionEvent

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex

import com.example.data.VRWindowConfig
import com.example.data.WindowContentType
import com.example.spatial.SpatialRenderer
import com.example.tracking.HandTracker
import com.example.ui.VRViewModel

import kotlin.math.*

data class WindowProjection(
    val windowId: String,
    val centerX: Float,
    val centerY: Float,
    val widthPx: Float,
    val heightPx: Float,
    val distance: Float
)

@Composable
fun VREnvironment(
    viewModel: VRViewModel,
    modifier: Modifier = Modifier,
    forceEditorMode: Boolean = false
) {
    val head by viewModel.headOrientation.collectAsState()
    val handState by viewModel.handState.collectAsState()
    val windows by viewModel.windowConfigs.collectAsState()
    val cameraActive by viewModel.backgroundCameraEnabled.collectAsState()
    val handStyle by viewModel.handStyle.collectAsState()
    val notifications by viewModel.notificationFeed.collectAsState()
    val grabbedId by viewModel.grabbedWindowId.collectAsState()
    val isMenuOpen by viewModel.isAppDrawerOpen.collectAsState()
    val editorModeState by viewModel.isEditorMode.collectAsState()
    val selectedId by viewModel.selectedWindowId.collectAsState()
    val actualEditorMode = forceEditorMode || editorModeState

    // Lens configuration from ViewModel (Single source of truth)
    val lensConfig by viewModel.lensConfig.collectAsState()

    var viewportSize by remember { mutableStateOf(IntSize.Zero) }
    val tone = remember { ToneGenerator(AudioManager.STREAM_MUSIC, 100) }
    val view = LocalView.current
    var pinchWasActive by remember { mutableStateOf(false) }

    data class GrabSnapshot(
        val handX: Float, val handY: Float,
        val winX: Float, val winY: Float, val winZ: Float
    )
    var grabSnapshot by remember { mutableStateOf<GrabSnapshot?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .onGloballyPositioned { viewportSize = it.size }
    ) {
        if (viewportSize.width == 0) return@Box
        val vw = viewportSize.width.toFloat()
        val vh = viewportSize.height.toFloat()
        val halfW = vw / 2f

        if (cameraActive) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                handTracker = viewModel.handTracker
            )
        } else {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color(0xFF040209), Color(0xFF0C071A), Color(0xFF040209))
                    )
                )
            }
        }

        val projections = remember(windows, head, vw, vh) {
            windows.values.map { win ->
                val (topLeft, bottomRight) = SpatialRenderer.windowToScreenMatrix(
                    window = win,
                    orientation = head,
                    screenWidth = vw.toInt(),
                    screenHeight = vh.toInt()
                )
                val widthPx = (bottomRight.x - topLeft.x).coerceAtLeast(0f)
                val heightPx = (bottomRight.y - topLeft.y).coerceAtLeast(0f)
                val centerX = topLeft.x + widthPx / 2f
                val centerY = topLeft.y + heightPx / 2f
                
                val dist = sqrt(win.worldX * win.worldX + win.worldY * win.worldY + win.worldZ * win.worldZ)
                
                WindowProjection(
                    windowId = win.id,
                    centerX = centerX,
                    centerY = centerY,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    distance = dist
                )
            }.filter { it.widthPx > 10f && it.heightPx > 10f }
        }

        val rayHit = remember(handState.isDetected, handState.x, handState.y, windows, head, vw, vh) {
            if (handState.isDetected) {
                val screenX = handState.x * vw
                val screenY = handState.y * vh
                SpatialRenderer.raycastWindows(
                    screenX = screenX,
                    screenY = screenY,
                    screenWidth = vw.toInt(),
                    screenHeight = vh.toInt(),
                    orientation = head,
                    windows = windows.values.toList()
                )
            } else null
        }

        val gesture = handState.gesture
        val pinchActive = gesture == HandTracker.GestureType.PINCH

        LaunchedEffect(pinchActive, rayHit?.windowId, grabbedId) {
            if (pinchActive && !pinchWasActive) {
                if (grabbedId == null && rayHit != null) {
                    if (actualEditorMode) {
                        viewModel.selectWindow(rayHit.windowId)
                        tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                    } else {
                        viewModel.startGrab(rayHit.windowId)
                        val win = windows[rayHit.windowId]
                        if (win != null) {
                            grabSnapshot = GrabSnapshot(
                                handX = handState.x,
                                handY = handState.y,
                                winX = win.worldX,
                                winY = win.worldY,
                                winZ = win.worldZ
                            )
                            tone.startTone(ToneGenerator.TONE_PROP_BEEP, 60)
                        }
                    }
                }
            } else if (!pinchActive && pinchWasActive) {
                if (!actualEditorMode) {
                    val snap = grabSnapshot
                    if (snap != null && grabbedId != null) {
                        val moved = (handState.x - snap.handX).let { it * it } + (handState.y - snap.handY).let { it * it } > 0.001f

                        if (!moved && rayHit != null && rayHit.windowId == grabbedId) {
                            dispatchClickToWindow(view, grabbedId, rayHit.screenUV.x, rayHit.screenUV.y, vw, vh)
                            tone.startTone(ToneGenerator.TONE_PROP_ACK, 50)
                        }
                        viewModel.endGrab()
                        grabSnapshot = null
                    }
                }
            }
            pinchWasActive = pinchActive
        }

        LaunchedEffect(handState.x, handState.y, grabbedId) {
            if (actualEditorMode) return@LaunchedEffect
            val snap = grabSnapshot
            val id = grabbedId
            if (snap != null && handState.isDetected && id != null) {
                val dx = handState.x - snap.handX
                val dy = handState.y - snap.handY
                val scale = 1.5f
                val newX = (snap.winX + dx * scale).coerceIn(-3f, 3f)
                val newY = (snap.winY - dy * scale).coerceIn(-2f, 2f)
                viewModel.moveWindowToWorld(id, newX, newY, snap.winZ)
            }
        }

        // Render Left Eye
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationX = -lensConfig.ipd / 2f + lensConfig.leftOffsetX
                    translationY = lensConfig.leftOffsetY
                    scaleX = lensConfig.leftScaleX
                    scaleY = lensConfig.leftScaleY
                    rotationX = lensConfig.verticalRotation
                    rotationY = lensConfig.horizontalRotation
                    // Note: lensConfig.distortionIntensity is prepared for future integration with SpatialRenderer
                }
                .clip(RoundedCornerShape(0.dp))
        ) {
            Box(Modifier.fillMaxSize().width((vw / 2f).dp)) {
                renderEyeContent(
                    projections = projections,
                    windows = windows,
                    rayHit = rayHit,
                    grabbedId = grabbedId,
                    selectedId = selectedId,
                    actualEditorMode = actualEditorMode,
                    isMenuOpen = isMenuOpen,
                    viewModel = viewModel
                )
            }
        }

        // Render Right Eye
        Box(
            modifier = Modifier
                .fillMaxSize()
                .offset(x = halfW.dp)
                .graphicsLayer {
                    translationX = lensConfig.ipd / 2f + lensConfig.rightOffsetX
                    translationY = lensConfig.rightOffsetY
                    scaleX = lensConfig.rightScaleX
                    scaleY = lensConfig.rightScaleY
                    rotationX = lensConfig.verticalRotation
                    rotationY = lensConfig.horizontalRotation
                    // Note: lensConfig.distortionIntensity is prepared for future integration with SpatialRenderer
                }
                .clip(RoundedCornerShape(0.dp))
        ) {
            Box(Modifier.fillMaxSize().width((vw / 2f).dp)) {
                renderEyeContent(
                    projections = projections,
                    windows = windows,
                    rayHit = rayHit,
                    grabbedId = grabbedId,
                    selectedId = selectedId,
                    actualEditorMode = actualEditorMode,
                    isMenuOpen = isMenuOpen,
                    viewModel = viewModel
                )
            }
        }

        // Overlay elements that are not eye-specific (hands, notifications, menus, editor panel)
        if (handState.isDetected) {
            RenderHandSkeletons(vw, vh, handState, handStyle)
            val cursorX = handState.x * vw
            val cursorY = handState.y * vh
            Box(
                Modifier
                    .offset(x = (cursorX - 12).dp, y = (cursorY - 12).dp)
                    .size(24.dp)
                    .graphicsLayer {
                        val s = if (pinchActive) 0.7f else 1f
                        scaleX = s
                        scaleY = s
                    }
                    .background(
                        Brush.radialGradient(
                            listOf(
                                if (pinchActive) Color(0xFF00FFCC) else Color.White,
                                Color.Transparent
                            )
                        ),
                        CircleShape
                    )
                    .border(
                        1.5.dp,
                        if (pinchActive) Color(0xFF00FFCC) else Color.White,
                        CircleShape
                    )
            )
        }

        if (notifications.isNotEmpty()) {
            val n = notifications.first()
            Box(
                Modifier.fillMaxWidth().padding(16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.widthIn(max = 300.dp)
                ) {
                    Row(
                        Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "[${n.app}] ${n.title}",
                                fontSize = 10.sp,
                                color = Color(0xFF00FFCC),
                                fontWeight = FontWeight.Bold
                            )
                            Text(n.body, fontSize = 10.sp, color = Color.White, maxLines = 2)
                        }
                        IconButton(
                            onClick = { viewModel.dismissNotification(n.id) },
                            Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // MENÚ VR - Controlado únicamente por gesto
        // ==========================================
        val isMenuGesture = gesture.toString().uppercase() == "MENU"
        val activeHand = handState.hands.firstOrNull()

        AnimatedVisibility(
            visible = isMenuGesture && activeHand != null,
            enter = fadeIn(animationSpec = tween(300)) +
                    scaleIn(initialScale = 0.8f, animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(200)) +
                    scaleOut(targetScale = 0.8f, animationSpec = tween(200))
        ) {

            val anchorX = activeHand?.menuAnchorX ?: 0.5f
            val anchorY = activeHand?.menuAnchorY ?: 0.5f

            // Posición del centro del menú
            val menuWidth = 150.dp
            val menuHeight = 180.dp

            val menuWidthPx = 150f
            val menuHeightPx = 180f

            val rawX = anchorX * vw
            val rawY = anchorY * vh

            // Mantener dentro de pantalla
            val posX = (rawX - menuWidthPx / 2f)
                .coerceIn(10f, vw - menuWidthPx - 10f)

            val posY = (rawY - menuHeightPx / 2f)
                .coerceIn(10f, vh - menuHeightPx - 10f)


            Box(
                modifier = Modifier
                    .offset(
                        x = posX.dp,
                        y = posY.dp
                    )
                    .zIndex(100f)
                    .width(menuWidth)
                    .height(menuHeight)
                    .background(
                        Color.Black.copy(alpha = 0.75f),
                        RoundedCornerShape(20.dp)
                    )
                    .border(
                        1.dp,
                        Color(0xFF00FFCC).copy(alpha = 0.8f),
                        RoundedCornerShape(20.dp)
                    )
                    .padding(12.dp)
            ) {

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Text(
                        "VR MENU",
                        color = Color(0xFF00FFCC),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    HorizontalDivider(
                        color = Color(0xFF00FFCC).copy(alpha = 0.3f)
                    )

                    Text(
                        "🏠 Inicio",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Text(
                        "⚙ Configuración",
                        color = Color.White,
                        fontSize = 12.sp
                    )

                    Text(
                        "✕ Cerrar",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }
            }
        } // Box del menú

        if (actualEditorMode) {
            EditorControlPanel(
                viewModel = viewModel,
                selectedWindow = windows[selectedId],
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 40.dp)
            )
            if (!forceEditorMode) {
                Button(
                    onClick = { viewModel.setEditorMode(false) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.85f)),
                    modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                ) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                    Spacer(Modifier.width(6.dp))
                    Text("SALIR EDITOR", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    } // Box principal
} // VREnvironment

@Composable
private fun renderEyeContent(
    projections: List<WindowProjection>,
    windows: Map<String, VRWindowConfig>,
    rayHit: SpatialRenderer.RaycastResult?,
    grabbedId: String?,
    selectedId: String?,
    actualEditorMode: Boolean,
    isMenuOpen: Boolean,
    viewModel: VRViewModel
) {
    projections.forEach { proj ->
        val win = windows[proj.windowId] ?: return@forEach
        val isHovered = rayHit?.windowId == proj.windowId
        val isGrabbed = grabbedId == proj.windowId
        val isSelected = selectedId == proj.windowId

        val borderColor = when {
            isSelected && actualEditorMode -> Color(0xFFFFAA00)
            isGrabbed -> Color(0xFF00FFCC)
            isHovered -> Color(0xFF00FFCC).copy(alpha = 0.6f)
            else -> Color.White.copy(alpha = 0.1f)
        }
        val borderWidth = when {
            isSelected && actualEditorMode -> 3.dp
            isGrabbed -> 2.dp
            isHovered -> 1.5.dp
            else -> 1.dp
        }

        if (proj.windowId == "universal_dock") {
            UniversalDock(
                viewModel = viewModel,
                isMenuOpen = isMenuOpen,
                theme = "glassmorphic",
                modifier = Modifier
                    .zIndex(10f)
                    .offset(
                        x = (proj.centerX - proj.widthPx / 2f).coerceAtLeast(0f).dp,
                        y = (proj.centerY - proj.heightPx / 2f).coerceAtLeast(0f).dp
                    )
                    .size(
                        proj.widthPx.coerceIn(280f, 900f).dp,
                        proj.heightPx.coerceIn(160f, 600f).dp
                    )
                    .graphicsLayer {
                        shadowElevation = (20f / proj.distance).coerceIn(2f, 20f)
                    }
            )
        } else {
            Box(
                modifier = Modifier
                    .offset(
                        x = (proj.centerX - proj.widthPx / 2f).coerceAtLeast(0f).dp,
                        y = (proj.centerY - proj.heightPx / 2f).coerceAtLeast(0f).dp
                    )
                    .size(
                        proj.widthPx.coerceIn(280f, 900f).dp,
                        proj.heightPx.coerceIn(160f, 600f).dp
                    )
                    .graphicsLayer {
                        shadowElevation = (20f / proj.distance).coerceIn(2f, 20f)
                    }
                    .background(
                        Color.Black.copy(alpha = 0.85f * win.brightness),
                        RoundedCornerShape(16.dp)
                    )
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(16.dp)
                    )
            ) {
                Column(Modifier.fillMaxSize()) {
                    WindowTitleBar(
                        title = win.title,
                        isPinned = win.isPinned,
                        isHovered = isHovered,
                        onClose = { /*if (!actualEditorMode) viewModel.closeWindow(win.id)*/ }
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .graphicsLayer { alpha = win.brightness }
                    ) {
                        VRWindowContent(
                            id = win.internalId.ifEmpty { win.id },
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EditorControlPanel(
    viewModel: VRViewModel,
    selectedWindow: VRWindowConfig?,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF00FFCC)),
        modifier = modifier.widthIn(max = 900.dp).padding(16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                "EDITOR DE ENTORNO VR",
                color = Color(0xFF00FFCC),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.sp
            )
            HorizontalDivider(color = Color(0xFF00FFCC).copy(alpha = 0.3f))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EditorButton(
                    text = "Añadir",
                    icon = Icons.Default.Add,
                    color = Color(0xFF00FFCC),
                    textColor = Color.Black,
                    onClick = { viewModel.addNewWindow() }
                )
                EditorButton(
                    text = "Eliminar",
                    icon = Icons.Default.Delete,
                    color = Color(0xFFFF4444),
                    textColor = Color.White,
                    enabled = selectedWindow != null && selectedWindow.id != "universal_dock",
                    onClick = { viewModel.deleteSelectedWindow() }
                )
                EditorButton(
                    text = "Guardar",
                    icon = Icons.Default.Check,
                    color = Color(0xFF6B4EFF),
                    textColor = Color.White,
                    onClick = { }
                )
            }
            if (selectedWindow != null) {
                HorizontalDivider(color = Color(0xFF00FFCC).copy(alpha = 0.3f))
                Text(
                    "▸ ${selectedWindow.title}",
                    fontSize = 14.sp,
                    color = Color(0xFFFFAA00),
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Posición:", color = Color.White, fontSize = 12.sp)
                    EditorIconButton(Icons.Default.ArrowBack, "X-") {
                        viewModel.updateSelectedWindow { copy(worldX = worldX - 0.1f) }
                    }
                    EditorIconButton(Icons.Default.ArrowForward, "X+") {
                        viewModel.updateSelectedWindow { copy(worldX = worldX + 0.1f) }
                    }
                    EditorIconButton(Icons.Default.ArrowUpward, "Y+") {
                        viewModel.updateSelectedWindow { copy(worldY = worldY + 0.1f) }
                    }
                    EditorIconButton(Icons.Default.ArrowDownward, "Y-") {
                        viewModel.updateSelectedWindow { copy(worldY = worldY - 0.1f) }
                    }
                    EditorIconButton(Icons.Default.Add, "Z-") {
                        viewModel.updateSelectedWindow { copy(worldZ = (worldZ - 0.1f).coerceAtLeast(0.5f)) }
                    }
                    EditorIconButton(Icons.Default.Remove, "Z+") {
                        viewModel.updateSelectedWindow { copy(worldZ = worldZ + 0.1f) }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tamaño:", color = Color.White, fontSize = 12.sp)
                    EditorSmallButton("+", Color(0xFF00FFCC)) {
                        viewModel.updateSelectedWindow {
                            copy(
                                widthMeters = widthMeters + 0.05f,
                                heightMeters = heightMeters + 0.05f
                            )
                        }
                    }
                    EditorSmallButton("-", Color(0xFF00FFCC)) {
                        viewModel.updateSelectedWindow {
                            copy(
                                widthMeters = (widthMeters - 0.05f).coerceAtLeast(0.2f),
                                heightMeters = (heightMeters - 0.05f).coerceAtLeast(0.15f)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Rotación:", color = Color.White, fontSize = 12.sp)
                    EditorSmallButton("↶", Color(0xFF6B4EFF)) {
                        viewModel.updateSelectedWindow { copy(yaw = yaw - 5f) }
                    }
                    EditorSmallButton("↷", Color(0xFF6B4EFF)) {
                        viewModel.updateSelectedWindow { copy(yaw = yaw + 5f) }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Brillo:", color = Color.White, fontSize = 12.sp)
                    EditorSmallButton("☀-", Color(0xFFFFAA00)) {
                        viewModel.updateSelectedWindow {
                            copy(brightness = (brightness - 0.1f).coerceIn(0.2f, 1.5f))
                        }
                    }
                    EditorSmallButton("☀+", Color(0xFFFFAA00)) {
                        viewModel.updateSelectedWindow {
                            copy(brightness = (brightness + 0.1f).coerceIn(0.2f, 1.5f))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Text("Contraste:", color = Color.White, fontSize = 12.sp)
                    EditorSmallButton("◐-", Color(0xFFFFAA00)) {
                        viewModel.updateSelectedWindow {
                            copy(contrast = (contrast - 0.1f).coerceIn(0.2f, 2f))
                        }
                    }
                    EditorSmallButton("◐+", Color(0xFFFFAA00)) {
                        viewModel.updateSelectedWindow {
                            copy(contrast = (contrast + 0.1f).coerceIn(0.2f, 2f))
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Contenido:", color = Color.White, fontSize = 12.sp)
                    WindowContentType.values().forEach { type ->
                        val isSelected = selectedWindow.contentType == type
                        EditorSmallButton(
                            text = type.name.take(8),
                            color = if (isSelected) Color(0xFF00FFCC) else Color.Gray.copy(alpha = 0.3f),
                            textColor = if (isSelected) Color.Black else Color.White
                        ) {
                            viewModel.updateSelectedWindow { copy(contentType = type) }
                        }
                    }
                }
            } else {
                Text(
                    "Selecciona una ventana haciendo pinch sobre ella",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }
    }
}

@Composable
private fun EditorButton(
    text: String,
    icon: ImageVector,
    color: Color,
    textColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        modifier = Modifier.height(44.dp)
    ) {
        Icon(icon, null, tint = textColor)
        Spacer(Modifier.width(4.dp))
        Text(text, color = textColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EditorIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .background(Color(0xFF00FFCC).copy(alpha = 0.15f), CircleShape)
    ) {
        Icon(icon, label, tint = Color(0xFF00FFCC))
    }
}

@Composable
private fun EditorSmallButton(
    text: String,
    color: Color,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.3f)),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        modifier = Modifier.height(32.dp)
    ) {
        Text(text, color = textColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

private fun dispatchClickToWindow(
    rootView: android.view.View,
    windowId: String?,
    u: Float,
    v: Float,
    vpW: Float,
    vpH: Float
) = Unit

private fun findViewByTagRecursive(root: android.view.View, tag: String): android.view.View? {
    if (root.tag == tag) return root
    if (root is android.view.ViewGroup) {
        for (i in 0 until root.childCount) {
            val found = findViewByTagRecursive(root.getChildAt(i), tag)
            if (found != null) return found
        }
    }
    return null
}

@Composable
private fun WindowTitleBar(
    title: String,
    isPinned: Boolean,
    isHovered: Boolean,
    onClose: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = if (isHovered) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.3f),
                shape = CircleShape,
                modifier = Modifier.size(8.dp)
            ) {}
            Spacer(Modifier.width(8.dp))
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(onClick = {}, Modifier.size(18.dp)) {
                Icon(
                    Icons.Default.PushPin, null,
                    tint = if (isPinned) Color(0xFF00FFCC) else Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
            IconButton(onClick = onClose, Modifier.size(18.dp)) {
                Icon(
                    Icons.Default.Close, null,
                    tint = Color.Red.copy(alpha = 0.8f),
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

@Composable
fun CameraPreview(modifier: Modifier = Modifier, handTracker: Any) {
    Canvas(modifier = modifier) {
        drawRect(Color.DarkGray)
    }
}

@Composable
fun VRWindowContent(id: String, viewModel: VRViewModel) {
    Box(Modifier.fillMaxSize().background(Color.DarkGray)) {
        Text("Content for $id", color = Color.White, modifier = Modifier.align(Alignment.Center))
    }
}