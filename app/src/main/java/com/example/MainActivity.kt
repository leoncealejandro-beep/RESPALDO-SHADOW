package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import kotlinx.coroutines.delay
import androidx.compose.foundation.Canvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VRViewModel
import com.example.ui.components.VREnvironment
import com.example.ui.theme.MyApplicationTheme
import kotlin.math.cos
import kotlin.math.sin

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val vrViewModel: VRViewModel = viewModel()

                DisposableEffect(Unit) {
                    vrViewModel.startTracking()

                    onDispose {
                        vrViewModel.stopTracking()
                    }
                }

                var cameraPermissionGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }

                var currentScreen by remember { mutableStateOf<Screen>(Screen.MainMenu) }
                var showLoading by remember { mutableStateOf(true) }

                val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
                DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                            cameraPermissionGranted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted -> cameraPermissionGranted = isGranted }

                if (showLoading) {
                    LoadingScreen(onFinished = { showLoading = false })
                } else {
                    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                        AnimatedContent(
                            targetState = currentScreen,
                            transitionSpec = {
                                fadeIn(tween(300)) + slideInHorizontally(
                                    initialOffsetX = { fullWidth -> fullWidth },
                                    animationSpec = tween(300)
                                ) togetherWith
                                        fadeOut(tween(300)) + slideOutHorizontally(
                                            targetOffsetX = { fullWidth -> -fullWidth },
                                            animationSpec = tween(300)
                                        )
                            }
                        ) { screen ->
                            when (screen) {
                                Screen.MainMenu -> {
                                    if (!cameraPermissionGranted) {
                                        OnboardingCalibrationScreen(
                                            onRequestPermission = {
                                                permissionLauncher.launch(Manifest.permission.CAMERA)
                                            }
                                        )
                                    } else {
                                        MainMenuScreen(
                                            onNavigate = { currentScreen = it },
                                            onStartVR = { currentScreen = Screen.VREnvironment },
                                            onOpenConfig = {
                                                val intent = Intent(context, VRConfigActivity::class.java)
                                                context.startActivity(intent)
                                            }
                                        )
                                    }
                                }
                                Screen.Settings -> SettingsScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Connection -> ConnectionScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Library -> LibraryScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Tools -> ToolsScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Games -> GamesScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Apps -> AppsScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.Diagnostics -> DiagnosticsScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.About -> AboutScreen(onBack = { currentScreen = Screen.MainMenu })
                                Screen.VREnvironment -> VREnvironment(viewModel = vrViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}


sealed class Screen {
    object MainMenu : Screen()
    object Settings : Screen()
    object Connection : Screen()
    object Library : Screen()
    object Tools : Screen()
    object Games : Screen()
    object Apps : Screen()
    object Diagnostics : Screen()
    object About : Screen()
    object VREnvironment : Screen()
}

// ============================================================================
// LOADING SCREEN
// ============================================================================
@Composable
fun LoadingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    
    // 5 seconds timer
    LaunchedEffect(Unit) {
        delay(5000)
        onFinished()
    }

    // Fade in animation for the whole screen
    var fadeIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        fadeIn = true
    }

    val screenAlpha by animateFloatAsState(
        targetValue = if (fadeIn) 1f else 0f,
        animationSpec = tween(1000),
        label = "screenAlpha"
    )

    // Breathing animation for the image
    val infiniteTransition = rememberInfiniteTransition(label = "loadingInfinite")
    val breathingScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathingScale"
    )

    // Animated alpha for the "Initializing..." text
    val textAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "textAlpha"
    )

    // Smooth progress indicator animation
    val progressWidth by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "progressWidth"
    )

    Box(
    modifier = Modifier
        .fillMaxSize()
        .alpha(screenAlpha)
) {

    // Imagen de fondo
    Image(
        painter = painterResource(id = R.drawable.carga),
        contentDescription = null,
        modifier = Modifier
            .fillMaxSize()
            .scale(breathingScale),
        contentScale = ContentScale.Crop
    )

    // Si quieres un oscurecido para que el texto sea legible
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.25f))
    )

   Column(
    modifier = Modifier
        .fillMaxSize()
        .padding(32.dp)
        .statusBarsPadding(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "SHADOW VR",
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        letterSpacing = 3.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Mixed Reality Desktop",
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF00FFCC),
        letterSpacing = 1.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    Text(
        text = "Initializing Virtual Environment...",
        fontSize = 12.sp,
        color = Color.White.copy(alpha = textAlpha),
        letterSpacing = 0.5.sp,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.weight(1f))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .background(
                Color.Black.copy(alpha = 0.3f),
                RoundedCornerShape(3.dp)
            )
            .border(
                1.dp,
                Color(0xFF00FFCC).copy(alpha = 0.3f),
                RoundedCornerShape(3.dp)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progressWidth)
                .height(6.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF00FFCC).copy(alpha = 0.8f),
                            Color(0xFF00FFCC),
                            Color.White
                        )
                    ),
                    RoundedCornerShape(3.dp)
                )
        )
    }

    }
} // <-- Cierra el Box
} // <-- Cierra LoadingScreen()

// ============================================================================
// PANTALLA PRINCIPAL
// ============================================================================
@Composable
fun MainMenuScreen(
    onNavigate: (Screen) -> Unit,
    onStartVR: () -> Unit,
    onOpenConfig: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }

    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 100f,
        animationSpec = tween(600, easing = EaseOutCubic),
        label = "offsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(600),
        label = "alpha"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()

        // Scroll state para permitir desplazamiento si el contenido es muy largo
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Envolvemos el título y el panel en un VerticalScroll
            Column(
                modifier = Modifier
                    .verticalScroll(scrollState)
                    .weight(1f), // Ocupa el espacio disponible pero permite scroll interno
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = offsetY.dp).alpha(alpha)
                ) {
                    Text(
                        text = "SHADOW VR",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "Mixed Reality Desktop",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF00FFCC),
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = "Immersive Computing Platform",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                GlassPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = offsetY.dp)
                        .alpha(alpha)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(), // Cambiado de fillMaxSize a fillMaxWidth para no forzar altura infinita
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MenuCard(
                            icon = Icons.Default.Vrpano,
                            title = "INICIAR VR",
                            subtitle = "Entrar al entorno de realidad virtual",
                            isPrimary = true,
                            onClick = onStartVR
                        )
                        // >>> BOTÓN CONFIGURACIONES <<<
                        MenuCard(
                            icon = Icons.Default.Tune,
                            title = "CONFIGURACIONES VR",
                            subtitle = "Editor de ventanas y entorno 3D",
                            isPrimary = true,
                            onClick = onOpenConfig
                        )
                        MenuCard(
                            icon = Icons.Default.Computer,
                            title = "Conectar PC",
                            subtitle = "Streaming de escritorio y juegos",
                            onClick = { onNavigate(Screen.Connection) }
                        )
                        MenuCard(
                            icon = Icons.Default.LibraryBooks,
                            title = "Biblioteca",
                            subtitle = "Tu colección de contenido",
                            onClick = { onNavigate(Screen.Library) }
                        )
                        MenuCard(
                            icon = Icons.Default.Settings,
                            title = "Configuración",
                            subtitle = "Ajustes del sistema",
                            onClick = { onNavigate(Screen.Settings) }
                        )
                        MenuCard(
                            icon = Icons.Default.Build,
                            title = "Herramientas",
                            subtitle = "Utilidades y diagnóstico",
                            onClick = { onNavigate(Screen.Tools) }
                        )
                        MenuCard(
                            icon = Icons.Default.SportsEsports,
                            title = "Juegos",
                            subtitle = "Experiencias interactivas",
                            onClick = { onNavigate(Screen.Games) }
                        )
                        MenuCard(
                            icon = Icons.Default.Apps,
                            title = "Aplicaciones",
                            subtitle = "Apps instaladas",
                            onClick = { onNavigate(Screen.Apps) }
                        )
                        MenuCard(
                            icon = Icons.Default.Analytics,
                            title = "Estado del Sistema",
                            subtitle = "Monitoreo en tiempo real",
                            onClick = { onNavigate(Screen.Diagnostics) }
                        )
                        MenuCard(
                            icon = Icons.Default.Info,
                            title = "Acerca de",
                            subtitle = "Información de la plataforma",
                            onClick = { onNavigate(Screen.About) }
                        )
                    }
                }
                
                // Espaciador inferior pequeño para dar aire al final del scroll
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
        
        // Indicador visual de scroll si es necesario (opcional, mejora UX)
        if (scrollState.canScrollForward || scrollState.canScrollBackward) {
             // El scrollbar nativo de compose aparecerá automáticamente si se usa verticalScroll
        }
    }
}

// ============================================================================
// El resto del MainActivity (AnimatedBackground, GlassPanel, MenuCard, etc.)
// se mantiene EXACTAMENTE igual que en tu código original.
// ============================================================================

@Composable
fun AnimatedBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "background")
    val offsetX by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(20000, easing = LinearEasing), RepeatMode.Restart),
        label = "offsetX"
    )
    val opacity by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(4000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "opacity"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(Color(0xFF0C0724), Color(0xFF1A0B3D), Color(0xFF03010C)))
        )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val time = offsetX / 1000f
            val particleColor1 = Color(0xFF00FFCC).copy(alpha = 0.3f * opacity)
            repeat(50) { i ->
                val x = (i * 137.5f + time * 50) % size.width
                val y = (i * 89.3f + sin(time + i) * 100) % size.height
                val radius = 2f + sin(time + i * 0.5f) * 1.5f
                drawCircle(color = particleColor1, radius = radius, center = Offset(x, y))
            }
            val particleColor2 = Color(0xFF6B4EFF).copy(alpha = 0.25f * opacity)
            repeat(30) { i ->
                val x = (i * 97.3f + time * 30) % size.width
                val y = (i * 157.7f + cos(time + i) * 80) % size.height
                val radius = 1.5f + cos(time + i * 0.7f) * 1f
                drawCircle(color = particleColor2, radius = radius, center = Offset(x, y))
            }
            val lineColor = Color(0xFF00FFCC).copy(alpha = 0.1f * opacity)
            repeat(20) { i ->
                val startX = (i * 173.9f + time * 40) % size.width
                val startY = (i * 113.1f) % size.height
                val endX = startX + 150f
                val endY = startY + sin(time + i) * 50f
                drawLine(color = lineColor, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 1f)
            }
        }
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFF6B4EFF).copy(alpha = 0.15f * opacity), Color.Transparent),
                    center = Offset(300f, 400f), radius = 500f
                )
            )
        )
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    listOf(Color(0xFF00FFCC).copy(alpha = 0.1f * opacity), Color.Transparent),
                    center = Offset(800f, 600f), radius = 600f
                )
            )
        )
    }
}

@Composable
fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val glowAnimation = rememberInfiniteTransition(label = "glow")
    val glowAlpha by glowAnimation.animateFloat(
        initialValue = 0.3f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "glowAlpha"
    )

    Box(
        modifier = modifier
            // Sombra coloreada para mantener el efecto glassmorphism sin desenfocar el contenido
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                ambientColor = Color(0xFF00FFCC).copy(alpha = 0.15f),
                spotColor = Color(0xFF6B4EFF).copy(alpha = 0.25f)
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.04f))))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(listOf(
                    Color(0xFF00FFCC).copy(alpha = glowAlpha),
                    Color(0xFF6B4EFF).copy(alpha = glowAlpha * 0.7f),
                    Color(0xFF00FFCC).copy(alpha = glowAlpha)
                )),
                shape = RoundedCornerShape(32.dp)
            )
            .drawBehind {
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.05f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(32.dp.toPx())
                )
            }
            // SE ELIMINÓ .blur(2.dp) PARA EVITAR QUE EL CONTENIDO SE VEA BORROSO
    ) {
        Column(modifier = Modifier.padding(32.dp).fillMaxWidth(), content = content)
    }
}

@Composable
fun MenuCard(
    icon: ImageVector, title: String, subtitle: String,
    isPrimary: Boolean = false, onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f), label = "scale"
    )
    val glowIntensity by animateFloatAsState(
        targetValue = if (isPressed) 0.8f else 0.4f,
        animationSpec = tween(200), label = "glow"
    )
    val cardHeight = if (isPrimary) 140.dp else 100.dp

    Card(
        modifier = Modifier.fillMaxWidth().height(cardHeight).scale(scale)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(
                    if (isPrimary) Brush.linearGradient(listOf(
                        Color(0xFF00FFCC).copy(alpha = 0.15f), Color(0xFF6B4EFF).copy(alpha = 0.1f)
                    ))
                    else Brush.verticalGradient(listOf(
                        Color.White.copy(alpha = 0.06f), Color.White.copy(alpha = 0.02f)
                    ))
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        if (isPrimary) listOf(
                            Color(0xFF00FFCC).copy(alpha = glowIntensity),
                            Color(0xFF6B4EFF).copy(alpha = glowIntensity * 0.8f)
                        ) else listOf(
                            Color.White.copy(alpha = 0.2f), Color.White.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier.size(if (isPrimary) 56.dp else 44.dp)
                        .background(
                            if (isPrimary) Color(0xFF00FFCC).copy(alpha = 0.2f)
                            else Color(0xFF6B4EFF).copy(alpha = 0.15f), CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon, contentDescription = null,
                        tint = if (isPrimary) Color(0xFF00FFCC) else Color(0xFF6B4EFF),
                        modifier = Modifier.size(if (isPrimary) 32.dp else 24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
                    Text(
                        text = title,
                        fontSize = if (isPrimary) 18.sp else 15.sp,
                        fontWeight = FontWeight.Bold, color = Color.White,
                        letterSpacing = if (isPrimary) 1.sp else 0.5.sp
                    )
                    if (subtitle.isNotEmpty()) {
                        Text(
                            text = subtitle,
                            fontSize = if (isPrimary) 12.sp else 11.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                if (isPrimary) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward, contentDescription = null,
                        tint = Color(0xFF00FFCC), modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

// ============================================================================
// Resto de pantallas (Settings, Connection, Library, Tools, Games, Apps,
// Diagnostics, About) + OnboardingCalibrationScreen
// Se mantienen EXACTAMENTE como en tu código original.
// ============================================================================

@Composable
fun GenericScreen(
    title: String, subtitle: String, icon: ImageVector,
    onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }
    val offsetY by animateFloatAsState(
        targetValue = if (isVisible) 0f else 50f,
        animationSpec = tween(500, easing = EaseOutCubic), label = "offsetY"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(500), label = "alpha"
    )
    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedBackground()
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, "Volver", tint = Color.White)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 1.sp)
                    Text(subtitle, fontSize = 11.sp, color = Color(0xFF00FFCC), letterSpacing = 0.5.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            GlassPanel(modifier = Modifier.fillMaxWidth().weight(1f).offset(y = offsetY.dp).alpha(alpha)) { content() }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable fun SettingsScreen(onBack: () -> Unit) = GenericScreen("CONFIGURACIÓN", "System Settings", Icons.Default.Settings, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem("Calibración de Cámara", "Ajustar seguimiento óptico")
        SettingItem("Rendimiento", "Optimizar FPS y calidad")
        SettingItem("Audio", "Configuración de sonido 3D")
        SettingItem("Red", "Conexiones WiFi y Bluetooth")
        SettingItem("Privacidad", "Permisos y datos")
        SettingItem("Actualizaciones", "Versión del sistema")
    }
}

@Composable fun ConnectionScreen(onBack: () -> Unit) = GenericScreen("CONECTAR PC", "PC Connection", Icons.Default.Computer, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        ConnectionStatus("Estado: Desconectado", false)
        SettingItem("Buscar PC en la red", "Escaneando dispositivos disponibles")
        SettingItem("Conexión manual", "Ingresar IP del PC")
        SettingItem("Streaming de escritorio", "Espejar pantalla del PC")
        SettingItem("Streaming de juegos", "Cloud gaming y game streaming")
        SettingItem("Latencia", "Optimizar conexión")
    }
}

@Composable fun LibraryScreen(onBack: () -> Unit) = GenericScreen("BIBLIOTECA", "Content Library", Icons.Default.LibraryBooks, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LibraryItem("Recientes", "12 elementos")
        LibraryItem("Favoritos", "8 elementos")
        LibraryItem("Descargas", "5 elementos")
        LibraryItem("Videos 360°", "3 elementos")
        LibraryItem("Experiencias VR", "15 elementos")
        LibraryItem("Aplicaciones MR", "7 elementos")
    }
}

@Composable fun ToolsScreen(onBack: () -> Unit) = GenericScreen("HERRAMIENTAS", "Utilities", Icons.Default.Build, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingItem("Calibración de manos", "Configurar tracking manual")
        SettingItem("Espacio de trabajo", "Definir área segura")
        SettingItem("Captura de pantalla", "Grabar sesiones VR")
        SettingItem("Transferencia de archivos", "Gestionar contenido")
        SettingItem("Modo desarrollador", "Opciones avanzadas")
        SettingItem("Logs del sistema", "Ver registros")
    }
}

@Composable fun GamesScreen(onBack: () -> Unit) = GenericScreen("JUEGOS", "Gaming Hub", Icons.Default.SportsEsports, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        GameItem("Beat Saber", "Ritmo y acción")
        GameItem("Superhot VR", "Shooter táctico")
        GameItem("Pavlov", "FPS multijugador")
        GameItem("Job Simulator", "Simulación humorística")
        GameItem("Rec Room", "Social y multijugador")
        GameItem("Explorar tienda", "Más juegos disponibles")
    }
}

@Composable fun AppsScreen(onBack: () -> Unit) = GenericScreen("APLICACIONES", "Installed Apps", Icons.Default.Apps, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        AppItem("Navegador VR", "Exploración web inmersiva")
        AppItem("Reproductor 360°", "Videos panorámicos")
        AppItem("Galería MR", "Fotos y videos en realidad mixta")
        AppItem("Escritorio Virtual", "Productividad en VR")
        AppItem("Social VR", "Plataforma social")
        AppItem("Fitness VR", "Ejercicios inmersivos")
    }
}

@Composable fun DiagnosticsScreen(onBack: () -> Unit) = GenericScreen("DIAGNÓSTICO", "System Monitor", Icons.Default.Analytics, onBack) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DiagnosticItem("CPU", "23%", 0.23f)
        DiagnosticItem("GPU", "45%", 0.45f)
        DiagnosticItem("RAM", "1.2 GB / 4 GB", 0.3f)
        DiagnosticItem("Batería", "78%", 0.78f)
        DiagnosticItem("Temperatura", "42°C", 0.42f)
        DiagnosticItem("FPS", "60/60", 1f)
        DiagnosticItem("Latencia", "12ms", 0.12f)
        DiagnosticItem("Red", "Excelente", 0.95f)
    }
}

@Composable fun AboutScreen(onBack: () -> Unit) = GenericScreen("ACERCA DE", "About Shadow VR", Icons.Default.Info, onBack) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        Text("SHADOW VR", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
        Text("Mixed Reality Desktop", fontSize = 14.sp, color = Color(0xFF00FFCC), letterSpacing = 1.sp)
        Text("Versión 1.0.0", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Plataforma de Realidad Virtual y Realidad Mixta para Android.\n\nTransforma tu teléfono en un visor VR de última generación con seguimiento de manos, cámara passthrough y experiencias inmersivas.",
            fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Desarrollado con tecnología de punta\npara la próxima generación de computación inmersiva.",
            fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), textAlign = TextAlign.Center, lineHeight = 18.sp
        )
    }
}

@Composable
fun SettingItem(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ConnectionStatus(text: String, connected: Boolean) {
    val containerColor = if (connected) Color(0xFF00FFCC).copy(alpha = 0.15f) else Color(0xFFFF4444).copy(alpha = 0.15f)
    val indicatorColor = if (connected) Color(0xFF00FFCC) else Color(0xFFFF4444)
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(12.dp).background(indicatorColor, CircleShape))
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
        }
    }
}

@Composable
fun LibraryItem(title: String, count: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(count, fontSize = 12.sp, color = Color(0xFF00FFCC))
        }
    }
}

@Composable
fun GameItem(title: String, genre: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(genre, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun AppItem(title: String, description: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White)
            Text(description, fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun DiagnosticItem(label: String, value: String, percentage: Float) {
    val animatedPercentage by animateFloatAsState(
        targetValue = percentage, animationSpec = tween(1000, easing = EaseOutCubic), label = "percentage"
    )
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.05f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White)
                Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00FFCC))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth().height(6.dp).background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(3.dp))) {
                Box(modifier = Modifier.fillMaxWidth(animatedPercentage).height(6.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0xFF00FFCC), Color(0xFF6B4EFF))), RoundedCornerShape(3.dp)))
            }
        }
    }
}

@Composable
fun OnboardingCalibrationScreen(onRequestPermission: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0C0724), Color(0xFF03010C)))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.width(340.dp)
                .border(androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)), RoundedCornerShape(24.dp))
                .background(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.02f))), RoundedCornerShape(24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            Surface(color = Color(0xFF00FFCC).copy(alpha = 0.15f), shape = RoundedCornerShape(16.dp), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.CameraAlt, "Camera", tint = Color(0xFF00FFCC), modifier = Modifier.size(28.dp))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("SHADOW VR", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 2.sp)
            Text("Mixed Reality Desktop", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF00FFCC), letterSpacing = 1.sp, modifier = Modifier.padding(top = 2.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "Place your Android phone inside a mobile VR headset or Cardboard viewer.\n\nTo see your environment in 3D pass-through and track hand gestures in space, we require back camera stream access.",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f), textAlign = TextAlign.Center, lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC), contentColor = Color.Black),
                shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()
            ) {
                Text("CALIBRATE & ENTER VR", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }
}