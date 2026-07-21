package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FilterCenterFocus
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.ui.VRViewModel
import com.example.ui.apps.AppRegistry
import com.example.ui.apps.NativeApp
import kotlinx.coroutines.delay

// =====================================================================
// Reusable glass primitives
// =====================================================================
private val Accent = Color(0xFF00FFCC)
private val GlassStroke = Color.White.copy(alpha = 0.10f)
private val GlassFill = Color.White.copy(alpha = 0.08f)
private val GlassFillActive = Color.White.copy(alpha = 0.20f)

@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    active: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (active) GlassFillActive else GlassFill
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (active) Accent.copy(alpha = 0.5f) else GlassStroke
        ),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
        )
    ) { content() }
}

@Composable
private fun GlassButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Accent,
    container: Color = Accent.copy(alpha = 0.2f),
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = tint),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = modifier.height(28.dp)
    ) { content() }
}

@Composable
private fun GlassIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    size: androidx.compose.ui.unit.Dp = 28.dp,
    iconSize: androidx.compose.ui.unit.Dp = 16.dp
) {
    IconButton(onClick = onClick, modifier = modifier.size(size)) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
private fun WindowHeader(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    status: String? = null
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        if (!subtitle.isNullOrEmpty()) {
            Spacer(Modifier.width(6.dp))
            Text(subtitle, fontSize = 10.sp, color = Accent)
        }
        Spacer(Modifier.weight(1f))
        if (status != null) {
            Text(status, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun VRTextField(
    value: String,
    placeholder: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    secure: Boolean = false
) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leadingIcon != null) { leadingIcon(); Spacer(Modifier.width(4.dp)) }
            val display = when {
                value.isEmpty() -> placeholder
                secure && value.isNotEmpty() -> "•".repeat(value.length.coerceAtMost(24))
                else -> value
            }
            Text(
                text = display,
                fontSize = 11.sp,
                color = if (value.isEmpty()) Color.Gray else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =====================================================================
// Window content switcher  🆕 con resolución dinámica de apps nativas
// =====================================================================
@Composable
fun VRWindowContent(
    id: String,
    viewModel: VRViewModel,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        val cleanId = id.trim()
        when (cleanId) {
            "launcher"   -> LauncherContent(windowId = id, viewModel = viewModel)
            "browser"    -> BrowserContent(windowId = id, viewModel = viewModel)
            "explorer"   -> ExplorerContent(windowId = id, viewModel = viewModel)
            "gallery"    -> GalleryContent(windowId = id, viewModel = viewModel)
            "media"      -> MediaPlayerContent(windowId = id, viewModel = viewModel)
            "assistant"  -> AssistantContent(windowId = id, viewModel = viewModel)
            "settings"   -> SettingsContent(windowId = id, viewModel = viewModel)
            "keyboard"   -> KeyboardContent(windowId = id, viewModel = viewModel)
            else -> {
                // 🆕 Fallback dinámico: consulta el registro de apps nativas
                val nativeApp = AppRegistry.getApp(cleanId)
                if (nativeApp != null) {
                    nativeApp.content(id, viewModel)
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("vr_window_$id"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("App Content Not Found: $cleanId", color = Color.White)
                    }
                }
            }
        }
    }
}

// =====================================================================
// 1. Launcher  🆕 con pestaña "Mis Apps"
// =====================================================================
@Composable
fun LauncherContent(windowId: String, viewModel: VRViewModel) {
    val windowConfigs by viewModel.windowConfigs.collectAsStateWithLifecycle()
    val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val recentApps by viewModel.recentApps.collectAsStateWithLifecycle()
    val notifications by viewModel.notificationFeed.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }

    val spatialApps = remember {
        listOf(
            AppItem("browser", "Web Browser", Icons.Default.Language, "Explore spatial web", "Productivity"),
            AppItem("explorer", "File Explorer", Icons.Default.Folder, "Manage files & notes", "Productivity"),
            AppItem("gallery", "Media Gallery", Icons.Default.Collections, "View spatial photos", "Media"),
            AppItem("media", "Cinema Widescreen", Icons.Default.Movie, "Curved theater player", "Media"),
            AppItem("assistant", "Gemini AI", Icons.Default.AutoAwesome, "Voice/chat smart helper", "AI"),
            AppItem("settings", "Preferences", Icons.Default.Settings, "Fine-tune VR layout", "System"),
            AppItem("keyboard", "Virtual Keys", Icons.Default.Keyboard, "Spatial input panel", "System")
        )
    }

    // 🆕 Nueva pestaña "Mis Apps" al final
    val tabs = remember { listOf("Spatial", "Apps", "Favorites", "Recent", "Alerts", "Mis Apps") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("vr_window_$windowId")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "SHADOW VR",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Mixed Reality Spatial Workspace",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
            GlassButton(
                onClick = { viewModel.recenterWorld() },
                modifier = Modifier.testTag("recenter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterCenterFocus,
                    contentDescription = "Recenter",
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Recenter", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tab bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Surface(
                    color = if (active) Accent.copy(alpha = 0.25f) else GlassFill,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (active) Accent else GlassStroke),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTab = idx }
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Accent else Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        when (selectedTab) {
            0 -> SpatialAppsGrid(spatialApps = spatialApps, windowConfigs = windowConfigs, viewModel = viewModel)
            1 -> AppDrawerGrid(installedApps = installedApps, viewModel = viewModel)
            2 -> FavoritesGrid(installedApps = installedApps, favoriteApps = favoriteApps, viewModel = viewModel)
            3 -> RecentGrid(installedApps = installedApps, recentApps = recentApps, viewModel = viewModel)
            4 -> NotificationFeed(notifications = notifications, viewModel = viewModel)
            5 -> NativeAppsGrid(viewModel = viewModel, windowConfigs = windowConfigs) // 🆕
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { viewModel.resetLayout() },
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "Reset Workspace", modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Reset Spatial Layout", fontSize = 11.sp, color = Color.White)
        }
    }
}

data class AppItem(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val desc: String,
    val category: String
)

@Composable
private fun SpatialAppsGrid(
    spatialApps: List<AppItem>,
    windowConfigs: Map<String, com.example.data.VRWindowConfig>,
    viewModel: VRViewModel
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(spatialApps.size) { idx ->
            val app = spatialApps[idx]
            val isOpen = windowConfigs[app.id]?.isOpen ?: false
            GlassCard(active = isOpen, onClick = { viewModel.toggleWindow(app.id, !isOpen) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = if (isOpen) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = app.icon,
                                contentDescription = app.name,
                                tint = if (isOpen) Accent else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            text = app.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.desc,
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.5f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppDrawerGrid(
    installedApps: List<VRViewModel.InstalledApp>,
    viewModel: VRViewModel
) {
    val query by viewModel.appDrawerSearchQuery.collectAsStateWithLifecycle()
    val filtered by remember(installedApps, query) {
        derivedStateOf {
            if (query.isEmpty()) installedApps
            else installedApps.filter {
                it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        VRTextField(
            value = query,
            placeholder = "Search apps...",
            onClick = { viewModel.focusKeyboardFor("app_drawer") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No applications found", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(filtered.size) { idx ->
                    InstalledAppCard(app = filtered[idx], viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun FavoritesGrid(
    installedApps: List<VRViewModel.InstalledApp>,
    favoriteApps: Set<String>,
    viewModel: VRViewModel
) {
    val favorites by remember(installedApps, favoriteApps) {
        derivedStateOf { installedApps.filter { it.packageName in favoriteApps } }
    }
    if (favorites.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No favorites yet. Star an app from the Apps tab.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(favorites.size) { idx ->
                InstalledAppCard(app = favorites[idx], viewModel = viewModel, showStar = true)
            }
        }
    }
}

@Composable
private fun RecentGrid(
    installedApps: List<VRViewModel.InstalledApp>,
    recentApps: List<String>,
    viewModel: VRViewModel
) {
    val recents by remember(installedApps, recentApps) {
        derivedStateOf { recentApps.mapNotNull { pkg -> installedApps.firstOrNull { it.packageName == pkg } } }
    }
    if (recents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No recent apps yet.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(recents.size) { idx ->
                InstalledAppCard(app = recents[idx], viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun InstalledAppCard(
    app: VRViewModel.InstalledApp,
    viewModel: VRViewModel,
    showStar: Boolean = false
) {
    val context = LocalContext.current
    val favoriteApps by viewModel.favoriteApps.collectAsStateWithLifecycle()
    val isFav = app.packageName in favoriteApps
    val appIcon = remember(app.packageName) {
        try { context.packageManager.getApplicationIcon(app.packageName) } catch (e: Exception) { null }
    }

    GlassCard(onClick = { viewModel.openExternalApp(app.packageName) }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (appIcon != null) {
                AsyncImage(
                    model = appIcon,
                    contentDescription = app.label,
                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                Surface(
                    color = Color.White.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Android,
                            contentDescription = app.label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.packageName,
                    fontSize = 8.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(
                onClick = { viewModel.toggleFavoriteApp(app.packageName) },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = if (isFav || showStar) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "Favorite",
                    tint = if (isFav) Accent else Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun NotificationFeed(
    notifications: List<VRViewModel.VRNotification>,
    viewModel: VRViewModel
) {
    if (notifications.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No notifications.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(notifications, key = { it.id }) { n ->
                Surface(
                    color = GlassFill,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlassStroke),
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.dismissNotification(n.id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(n.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            Text(
                                n.body,
                                fontSize = 10.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(n.app, fontSize = 8.sp, color = Accent.copy(alpha = 0.8f))
                        }
                        GlassIconButton(
                            icon = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            onClick = { viewModel.dismissNotification(n.id) },
                            size = 24.dp,
                            iconSize = 12.dp,
                            tint = Color.White.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 🆕 Grid de apps nativas registradas (pestaña "Mis Apps")
// =====================================================================
@Composable
private fun NativeAppsGrid(
    viewModel: VRViewModel,
    windowConfigs: Map<String, com.example.data.VRWindowConfig>
) {
    val nativeApps = remember { AppRegistry.getAllApps() }

    if (nativeApps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "No hay apps nativas registradas.\nAñádelas en AppRegistry.registerAll()",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(nativeApps.size) { idx ->
                val app = nativeApps[idx]
                val isOpen = windowConfigs[app.id]?.isOpen ?: false
                NativeAppCard(app = app, isOpen = isOpen, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun NativeAppCard(
    app: NativeApp,
    isOpen: Boolean,
    viewModel: VRViewModel
) {
    GlassCard(active = isOpen, onClick = { viewModel.toggleWindow(app.id, !isOpen) }) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                color = if (isOpen) Accent.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                shape = CircleShape,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = app.icon,
                        contentDescription = app.name,
                        tint = if (isOpen) Accent else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.desc,
                    fontSize = 10.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = app.category.uppercase(),
                    fontSize = 8.sp,
                    color = Accent.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// =====================================================================
// 2. Browser
// =====================================================================
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserContent(windowId: String, viewModel: VRViewModel) {
    val currentUrl by viewModel.currentUrl.collectAsStateWithLifecycle()
    val browserTabs by viewModel.browserTabs.collectAsStateWithLifecycle()
    val activeTabIdx by viewModel.activeTabIdx.collectAsStateWithLifecycle()
    val keyboardInput by viewModel.keyboardInput.collectAsStateWithLifecycle()
    val keyboardTarget by viewModel.keyboardTargetWindow.collectAsStateWithLifecycle()
    val inputUrl by remember(currentUrl, keyboardInput, keyboardTarget) {
        derivedStateOf { if (keyboardTarget == "browser") keyboardInput else currentUrl }
    }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(activeTabIdx, keyboardTarget) {
        if (keyboardTarget == "browser") viewModel.setKeyboardInput(currentUrl)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            browserTabs.forEachIndexed { index, url ->
                val isActive = index == activeTabIdx
                Surface(
                    color = if (isActive) GlassFillActive else GlassFill,
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
                    border = BorderStroke(1.dp, GlassStroke),
                    modifier = Modifier.weight(1f).clickable { viewModel.selectBrowserTab(index) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = url.replace("https://", "").replace("www.", ""),
                            fontSize = 10.sp,
                            color = if (isActive) Accent else Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (browserTabs.size > 1) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                tint = Color.White.copy(alpha = 0.5f),
                                modifier = Modifier.size(12.dp).clickable { viewModel.closeBrowserTab(index) }
                            )
                        }
                    }
                }
            }
            GlassIconButton(
                icon = Icons.Default.Add,
                contentDescription = "New Tab",
                onClick = { viewModel.addBrowserTab() },
                tint = Color.White
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.Default.Home,
                contentDescription = "Home",
                onClick = { viewModel.navigateBrowser("https://www.google.com") },
                size = 24.dp,
                iconSize = 16.dp,
                tint = Color.White
            )
            Spacer(Modifier.width(4.dp))
            VRTextField(
                value = inputUrl,
                placeholder = "Search or enter address",
                onClick = {
                    viewModel.focusKeyboardFor("browser")
                    viewModel.setKeyboardInput(currentUrl)
                },
                modifier = Modifier.weight(1f),
                leadingIcon = {
                    if (currentUrl.startsWith("https://")) {
                        Icon(Icons.Default.Lock, "Secure", tint = Accent, modifier = Modifier.size(12.dp))
                    }
                }
            )
            Spacer(Modifier.width(4.dp))
            if (isLoading) {
                CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
            } else {
                GlassIconButton(
                    icon = Icons.Default.ArrowForward,
                    contentDescription = "Go",
                    onClick = { viewModel.navigateBrowser(inputUrl) },
                    size = 24.dp,
                    iconSize = 16.dp,
                    tint = Accent
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(12.dp)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            allowFileAccess = true
                            allowContentAccess = true
                            mediaPlaybackRequiresUserGesture = false
                            javaScriptCanOpenWindowsAutomatically = true
                            loadsImagesAutomatically = true
                            blockNetworkImage = false
                            blockNetworkLoads = false
                            @Suppress("DEPRECATION")
                            allowFileAccessFromFileURLs = false
                            @Suppress("DEPRECATION")
                            allowUniversalAccessFromFileURLs = false
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                if (url != null && url != view?.url) return
                                if (url != null && url != viewModel.currentUrl.value) {
                                    viewModel.currentUrl.value = url
                                }
                            }
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean = false
                        }
                        loadUrl(currentUrl)
                    }
                },
                update = { webView ->
                    val target = viewModel.currentUrl.value
                    if (webView.url != target) webView.loadUrl(target)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

// =====================================================================
// 3. File Explorer
// =====================================================================
@Composable
fun ExplorerContent(windowId: String, viewModel: VRViewModel) {
    val currentPath by viewModel.currentPath.collectAsStateWithLifecycle()
    val filesList by viewModel.filesList.collectAsStateWithLifecycle()
    val keyboardInput by viewModel.keyboardInput.collectAsStateWithLifecycle()
    val keyboardTarget by viewModel.keyboardTargetWindow.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var isNewFolder by remember { mutableStateOf(true) }
    var newFileName by remember { mutableStateOf("") }

    LaunchedEffect(keyboardInput, keyboardTarget, showCreateDialog) {
        if (showCreateDialog && keyboardTarget == "explorer") newFileName = keyboardInput
    }
    LaunchedEffect(showCreateDialog) {
        if (showCreateDialog) {
            viewModel.focusKeyboardFor("explorer")
            viewModel.setKeyboardInput(newFileName)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                GlassIconButton(
                    icon = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    onClick = { viewModel.upOneFolder() },
                    size = 32.dp,
                    tint = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = currentPath.substringAfterLast("files/").ifEmpty { "Root" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 240.dp)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(
                    onClick = {
                        isNewFolder = true
                        newFileName = "New Folder"
                        showCreateDialog = true
                    },
                    container = Color.White.copy(alpha = 0.15f),
                    tint = Color.White
                ) {
                    Icon(Icons.Default.CreateNewFolder, "New Folder", modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Folder", fontSize = 10.sp, color = Color.White)
                }
                GlassButton(
                    onClick = {
                        isNewFolder = false
                        newFileName = "spatial_note.txt"
                        showCreateDialog = true
                    },
                    container = Color.White.copy(alpha = 0.15f),
                    tint = Color.White
                ) {
                    Icon(Icons.Default.NoteAdd, "New File", modifier = Modifier.size(14.dp), tint = Color.White)
                    Spacer(Modifier.width(4.dp))
                    Text("Note", fontSize = 10.sp, color = Color.White)
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (filesList.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("This folder is empty", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                    }
                }
            } else {
                items(filesList, key = { it.path }) { file ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                            .clickable {
                                if (file.isDirectory) viewModel.navigateToDirectory(file.path)
                                else viewModel.speak("Opening ${file.name}")
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (file.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                                contentDescription = file.name,
                                tint = if (file.isDirectory) Color(0xFFFFCC33) else Accent,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                Text(file.size, fontSize = 9.sp, color = Color.White.copy(alpha = 0.5f))
                            }
                        }
                        GlassIconButton(
                            icon = Icons.Default.Delete,
                            contentDescription = "Delete",
                            onClick = { viewModel.deleteFile(file.path) },
                            size = 24.dp,
                            iconSize = 14.dp,
                            tint = Color.Red.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        if (showCreateDialog) {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = if (isNewFolder) "Create Spatial Folder" else "Create Text Note",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    VRTextField(
                        value = newFileName,
                        placeholder = "Enter name...",
                        onClick = {
                            viewModel.focusKeyboardFor("explorer")
                            viewModel.setKeyboardInput(newFileName)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateDialog = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newFileName.isNotEmpty()) {
                                    if (isNewFolder) viewModel.createFolder(newFileName)
                                    else viewModel.createFile(newFileName, "Created inside Shadow VR on 2026.")
                                    showCreateDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Accent)
                        ) {
                            Text("Create", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// =====================================================================
// 4. Gallery
// =====================================================================
@Composable
fun GalleryContent(windowId: String, viewModel: VRViewModel) {
    val images by viewModel.galleryImages.collectAsStateWithLifecycle()
    val selectedIdx by viewModel.selectedGalleryIdx.collectAsStateWithLifecycle()
    var slideshowActive by remember { mutableStateOf(false) }

    LaunchedEffect(slideshowActive, images.size) {
        if (slideshowActive && images.isNotEmpty()) {
            while (slideshowActive) {
                delay(3000)
                val next = (selectedIdx + 1) % images.size
                viewModel.selectedGalleryIdx.value = next
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        if (selectedIdx == -1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Immersive Pictures", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                GlassIconButton(
                    icon = Icons.Default.PlayArrow,
                    contentDescription = "Play Slideshow",
                    onClick = {
                        if (images.isNotEmpty()) {
                            viewModel.selectedGalleryIdx.value = 0
                            slideshowActive = true
                        }
                    },
                    size = 32.dp,
                    tint = Accent
                )
            }
            Spacer(Modifier.height(10.dp))
            if (images.isEmpty()) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No pictures available.", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(images) { idx, url ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.selectedGalleryIdx.value = idx }
                        ) {
                            AsyncImage(
                                model = url,
                                contentDescription = "Gallery item $idx",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            val safeIdx = selectedIdx.coerceIn(0, (images.size - 1).coerceAtLeast(0))
            val currentImage = images.getOrNull(safeIdx)
            Box(modifier = Modifier.weight(1f)) {
                if (currentImage != null) {
                    AsyncImage(
                        model = currentImage,
                        contentDescription = "Expanded visual",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(14.dp))
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No image", color = Color.White)
                    }
                }
                GlassIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = "Close Preview",
                    onClick = {
                        viewModel.selectedGalleryIdx.value = -1
                        slideshowActive = false
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                )
                if (slideshowActive) {
                    Surface(
                        color = Accent.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    ) {
                        Text(
                            text = "SLIDESHOW ACTIVE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (images.size > 1) {
                    Row(
                        modifier = Modifier.fillMaxWidth().align(Alignment.Center).padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        GlassIconButton(
                            icon = Icons.Default.ChevronLeft,
                            contentDescription = "Prev",
                            onClick = {
                                viewModel.selectedGalleryIdx.value = (safeIdx - 1 + images.size) % images.size
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        )
                        GlassIconButton(
                            icon = Icons.Default.ChevronRight,
                            contentDescription = "Next",
                            onClick = {
                                viewModel.selectedGalleryIdx.value = (safeIdx + 1) % images.size
                            },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.6f), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 5. Media Player
// =====================================================================
@Composable
fun MediaPlayerContent(windowId: String, viewModel: VRViewModel) {
    val isPlaying by viewModel.isMediaPlaying.collectAsStateWithLifecycle()
    var playerMode by remember { mutableStateOf("Curved Widescreen") }
    var videoProgress by remember { mutableFloatStateOf(0.35f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (isPlaying) {
                delay(1000)
                val next = videoProgress + 0.01f
                if (next >= 1f) {
                    videoProgress = 1f
                    viewModel.isMediaPlaying.value = false
                    break
                } else {
                    videoProgress = next
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Curved Widescreen", "Flat Panel", "Cinema Dome").forEach { mode ->
                val isSel = playerMode == mode
                Surface(
                    color = if (isSel) Accent.copy(alpha = 0.25f) else GlassFill,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, if (isSel) Accent else GlassStroke),
                    modifier = Modifier.weight(1f).clickable { playerMode = mode }
                ) {
                    Text(
                        text = mode,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Accent else Color.White,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .blur(16.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF005577).copy(alpha = 0.4f),
                                Color.Transparent
                            ),
                            radius = 400f
                        )
                    )
            )
            AsyncImage(
                model = "https://images.unsplash.com/photo-1536440136628-849c177e76a1?w=800",
                contentDescription = "Video Canvas",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().align(Alignment.Center)
            )
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
            ) {
                Text(
                    text = "MODE: $playerMode • STREAMING HD",
                    fontSize = 9.sp,
                    color = Accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            if (!isPlaying) {
                IconButton(
                    onClick = {
                        if (videoProgress >= 1f) videoProgress = 0f
                        viewModel.isMediaPlaying.value = true
                    },
                    modifier = Modifier.size(56.dp).background(Accent, CircleShape).align(Alignment.Center)
                ) {
                    Icon(Icons.Default.PlayArrow, "Play", tint = Color.Black, modifier = Modifier.size(32.dp))
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("02:14", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
            Slider(
                value = videoProgress,
                onValueChange = { videoProgress = it },
                colors = SliderDefaults.colors(activeTrackColor = Accent, thumbColor = Accent),
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text("06:42", fontSize = 9.sp, color = Color.White.copy(alpha = 0.6f))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassIconButton(
                icon = Icons.Default.SkipPrevious,
                contentDescription = "Restart",
                onClick = { videoProgress = 0f },
                tint = Color.White
            )
            IconButton(
                onClick = {
                    if (!isPlaying && videoProgress >= 1f) videoProgress = 0f
                    viewModel.isMediaPlaying.value = !isPlaying
                },
                modifier = Modifier.size(40.dp).background(Color.White.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Toggle",
                    tint = Accent
                )
            }
            GlassIconButton(
                icon = Icons.Default.PowerSettingsNew,
                contentDescription = "Turn off Screen",
                onClick = { viewModel.toggleWindow("media", false) },
                tint = Color.Red.copy(alpha = 0.7f)
            )
        }
    }
}

// =====================================================================
// 6. Assistant
// =====================================================================
@Composable
fun AssistantContent(windowId: String, viewModel: VRViewModel) {
    val messages by viewModel.assistantMessages.collectAsStateWithLifecycle()
    val query by viewModel.assistantQuery.collectAsStateWithLifecycle()
    val isLoading by viewModel.isAssistantLoading.collectAsStateWithLifecycle()
    val keyboardInput by viewModel.keyboardInput.collectAsStateWithLifecycle()
    val keyboardTarget by viewModel.keyboardTargetWindow.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    LaunchedEffect(keyboardInput, keyboardTarget) {
        if (keyboardTarget == "assistant") viewModel.assistantQuery.value = keyboardInput
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        WindowHeader(title = "Gemini VR Helper", icon = Icons.Default.AutoAwesome, subtitle = "• Online")
        Spacer(Modifier.height(10.dp))
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages, key = { it.timestamp }) { msg ->
                val isMe = msg.sender == "user"
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                ) {
                    Surface(
                        color = if (isMe) Accent.copy(alpha = 0.25f) else GlassFill,
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 12.dp
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isMe) Accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f)
                        )
                    ) {
                        Text(
                            text = msg.text,
                            fontSize = 11.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                        )
                    }
                    Text(
                        text = if (isMe) "You" else "Gemini",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            if (isLoading) {
                item {
                    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Gemini is thinking...", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            VRTextField(
                value = query,
                placeholder = "Ask Gemini or type VR command...",
                onClick = {
                    viewModel.focusKeyboardFor("assistant")
                    viewModel.setKeyboardInput(query)
                },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendAssistantMessage() },
                modifier = Modifier.background(Accent, CircleShape).size(36.dp)
            ) {
                Icon(Icons.Default.Send, "Send", tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

// =====================================================================
// 7. Settings
// =====================================================================
@Composable
fun SettingsContent(windowId: String, viewModel: VRViewModel) {
    val ipdValue by viewModel.ipd.collectAsStateWithLifecycle()
    val lensValue by viewModel.lensCorrection.collectAsStateWithLifecycle()
    val cameraActive by viewModel.backgroundCameraEnabled.collectAsStateWithLifecycle()
    val activeTheme by viewModel.visualTheme.collectAsStateWithLifecycle()
    val activeHandStyle by viewModel.handStyle.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp).testTag("vr_window_$windowId")
    ) {
        Text("Preferences & Calibration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(Modifier.height(10.dp))

        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Interpupillary Distance (IPD)", fontSize = 11.sp, color = Color.White)
                Text(
                    "${ipdValue.toInt()} mm",
                    fontSize = 11.sp,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = ipdValue,
                onValueChange = { viewModel.ipd.value = it },
                valueRange = 55f..75f,
                colors = SliderDefaults.colors(activeTrackColor = Accent, thumbColor = Accent)
            )
        }

        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lens Correction Warp Factor", fontSize = 11.sp, color = Color.White)
                Text(
                    String.format("%.2f", lensValue),
                    fontSize = 11.sp,
                    color = Accent,
                    fontWeight = FontWeight.Bold
                )
            }
            Slider(
                value = lensValue,
                onValueChange = { viewModel.lensCorrection.value = it },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(activeTrackColor = Accent, thumbColor = Accent)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("MR Pass-Through Camera", fontSize = 11.sp, color = Color.White)
                Text("Real-time surroundings background", fontSize = 8.sp, color = Color.White.copy(alpha = 0.5f))
            }
            Switch(
                checked = cameraActive,
                onCheckedChange = { viewModel.backgroundCameraEnabled.value = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Accent,
                    checkedTrackColor = Accent.copy(alpha = 0.4f)
                )
            )
        }

        Spacer(Modifier.height(8.dp))

        Column {
            Text("Spatial UI Theme", fontSize = 11.sp, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("glassmorphic", "cosmic_dark", "ambient_warm").forEach { th ->
                    val isSel = activeTheme == th
                    Surface(
                        color = if (isSel) Accent.copy(alpha = 0.25f) else GlassFill,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSel) Accent else GlassStroke),
                        modifier = Modifier.weight(1f).clickable { viewModel.visualTheme.value = th }
                    ) {
                        Text(
                            text = th.replace("_", " ").uppercase(),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Accent else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Column {
            Text("Hand Tracking Render Style", fontSize = 11.sp, color = Color.White)
            Spacer(Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("skeleton", "transparent", "solid", "holographic").forEach { style ->
                    val isSel = activeHandStyle == style
                    Surface(
                        color = if (isSel) Accent.copy(alpha = 0.25f) else GlassFill,
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, if (isSel) Accent else GlassStroke),
                        modifier = Modifier.weight(1f).clickable { viewModel.handStyle.value = style }
                    ) {
                        Text(
                            text = when (style) {
                                "skeleton" -> "ESQUELETO"
                                "transparent" -> "TRANSP."
                                "solid" -> "SÓLIDA"
                                "holographic" -> "HOLOGRAM"
                                else -> style.uppercase()
                            },
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Accent else Color.White,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================
// 8. Keyboard
// =====================================================================
@Composable
fun KeyboardContent(windowId: String, viewModel: VRViewModel) {
    val currentInput by viewModel.keyboardInput.collectAsStateWithLifecycle()
    val isShift by viewModel.isShiftActive.collectAsStateWithLifecycle()
    val rows = remember {
        listOf(
            listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
            listOf("A", "S", "D", "F", "G", "H", "J", "K", "L", "-"),
            listOf("Shift", "Z", "X", "C", "V", "B", "N", "M", ",", "⌫"),
            listOf("Space", "Enter")
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(8.dp).testTag("vr_window_$windowId")
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.5f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isShift) {
                    Surface(
                        color = Accent.copy(alpha = 0.25f),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.padding(end = 6.dp)
                    ) {
                        Text(
                            "⇧",
                            fontSize = 10.sp,
                            color = Accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = currentInput.ifEmpty { "Hover/pinch keys to type..." },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = if (currentInput.isEmpty()) Color.Gray else Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { char ->
                    val weight = when (char) {
                        "Space" -> 2.5f
                        "Enter" -> 1.5f
                        "Shift" -> 1.2f
                        "⌫" -> 1.2f
                        else -> 1f
                    }
                    val containerColor = when {
                        char == "Enter" -> Accent
                        char == "Shift" && isShift -> Accent.copy(alpha = 0.8f)
                        char == "Space" -> Color.White.copy(alpha = 0.15f)
                        char == "⌫" -> Color.Red.copy(alpha = 0.25f)
                        else -> GlassFill
                    }
                    val textColor = when (char) {
                        "Enter" -> Color.Black
                        "Shift" -> if (isShift) Color.Black else Color.White
                        else -> Color.White
                    }
                    val label = when {
                        char.length == 1 && isShift && char != "⌫" && char != "-" && char != "," ->
                            char.uppercase()
                        else -> char
                    }
                    Card(
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(weight).clickable { viewModel.appendKey(char) }
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
                        }
                    }
                }
            }
        }
    }
}