package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.VRViewModel

@Composable
fun UniversalDock(
    viewModel: VRViewModel,
    isMenuOpen: Boolean,
    theme: String,
    modifier: Modifier = Modifier // <-- AÑADIDO: Permite control externo desde el entorno 3D
) {
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val recentApps by viewModel.recentApps.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val windowConfigs by viewModel.windowConfigs.collectAsState()
    var expanded by remember { mutableStateOf(isMenuOpen) }

    val context = LocalContext.current

    // Apps a mostrar: favoritas + recientes (sin duplicar)
    val dockApps = remember(favoriteApps, recentApps) {
        val fav = favoriteApps.toList()
        val rec = recentApps.filter { it !in fav }
        (fav + rec).take(8)
    }

    Box(
        // <-- CAMBIADO: Usar widthIn y el modifier externo en lugar de fillMaxWidth
        modifier = modifier.widthIn(max = 650.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.Black.copy(alpha = 0.75f),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.25f)),
            modifier = Modifier.wrapContentWidth().height(72.dp) // <-- CAMBIADO: wrapContentWidth
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Botón menú (app drawer)
                IconButton(onClick = { expanded = !expanded; viewModel.isAppDrawerOpen.value = expanded }, modifier = Modifier.size(44.dp)) {
                    Icon(imageVector = if (expanded) Icons.Default.Close else Icons.Default.Apps, contentDescription = "App Drawer", tint = Color(0xFF00FFCC), modifier = Modifier.size(22.dp))
                }

                // Apps en el dock
                LazyRow(modifier = Modifier.weight(1f).padding(horizontal = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    items(dockApps.size) { idx ->
                        val pkg = dockApps[idx]
                        val label = installedApps.firstOrNull { it.packageName == pkg }?.label ?: pkg
                        val icon = remember(pkg) {
                            try { context.packageManager.getApplicationIcon(pkg) } catch (e: Exception) { null }
                        }
                        val isOpen = windowConfigs["app_$pkg"]?.isOpen ?: false
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { viewModel.openExternalApp(pkg) }) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, if (isOpen) Color(0xFF00FFCC) else Color.White.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (icon != null) {
                                    AsyncImage(model = icon, contentDescription = label, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)))
                                } else {
                                    Icon(Icons.Default.Android, null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Text(label, fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 60.dp))
                        }
                    }
                }

                // Botones rápidos
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = { viewModel.recenterWorld() }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.FilterCenterFocus, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { viewModel.openInternalWindow("settings") }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Settings, null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        // App drawer expandido
        if (expanded) {
            Surface(
                color = Color.Black.copy(alpha = 0.95f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF00FFCC).copy(alpha = 0.3f)),
                // <-- CAMBIADO: Alineado al fondo del Box padre para que "salga" del dock en 3D
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp).widthIn(max = 520.dp).heightIn(max = 420.dp)
            ) {
                Column(Modifier.padding(16.dp).fillMaxSize()) {
                    Text("All Applications", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(Modifier.height(10.dp))
                    Text("Select an app to launch...", color = Color.Gray)
                    // Aquí podrías añadir una LazyColumn con installedApps
                }
            }
        }
    }
}