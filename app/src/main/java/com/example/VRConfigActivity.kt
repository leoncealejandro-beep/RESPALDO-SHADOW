package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.ui.components.GlassPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.core.content.ContextCompat
import androidx.compose.runtime.collectAsState
import com.example.ui.VRViewModel
import com.example.ui.theme.MyApplicationTheme

class VRConfigActivity : ComponentActivity() {
    private val vrViewModel: VRViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                var cameraPermissionGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context, Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                }
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    cameraPermissionGranted = isGranted
                }
                LaunchedEffect(Unit) {
                    if (!cameraPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }
                Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
                    if (cameraPermissionGranted) {
                        VRConfigScreen(viewModel = vrViewModel)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vrViewModel.startTracking()
    }

    override fun onPause() {
        vrViewModel.stopTracking()
        super.onPause()
    }
}

@Composable
fun VRConfigScreen(viewModel: VRViewModel) {

    val lensConfig by viewModel.lensConfig.collectAsState()

    val ipd = lensConfig.ipd

    val leftScale = lensConfig.leftScaleX
    val rightScale = lensConfig.rightScaleX

    val leftPosX = lensConfig.leftOffsetX
    val leftPosY = lensConfig.leftOffsetY

    val rightPosX = lensConfig.rightOffsetX
    val rightPosY = lensConfig.rightOffsetY

    val lensCorrection = lensConfig.distortionIntensity

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0724)),
        contentAlignment = Alignment.Center
    ) {
        GlassPanel(
            modifier = Modifier
                .width(520.dp)
                .wrapContentHeight()
                .graphicsLayer {
                    cameraDistance = 1200f
                    rotationX = -3f
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "CONFIGURACIÓN VR",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Ajusta la posición y escala de los paneles",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                )

                Text(
                    text = "Vista previa:",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val ipdSpacing = (((ipd - 45f) / 30f) * 100f + 16f).dp

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConfigurablePanel(
                        title = "LENTE IZQUIERDO",
                        rotationY = 5f,
                        scaleX = lensConfig.leftScaleX,
                        scaleY = lensConfig.leftScaleY,
                        posX = leftPosX,
                        posY = leftPosY,
                        rotationZ = 0f
                    )
                    
                    Spacer(modifier = Modifier.width(ipdSpacing))
                    
                    ConfigurablePanel(
    title = "LENTE DERECHO",
    rotationY = -5f,
    scaleX = lensConfig.rightScaleX,
    scaleY = lensConfig.rightScaleY,
    posX = rightPosX,
    posY = rightPosY,
    rotationZ = 0f
)
                }

                Spacer(modifier = Modifier.height(32.dp))

                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    VRControlRow(
                        label = "IPD",
                        valueText = "${ipd.toInt()} mm",
                        content = {
                            Slider(
                                value = ipd,
                                onValueChange = { viewModel.updateIPD(it) },
                                valueRange = 45f..75f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                )
                            )
                        }
                    )

                    VRControlRow(
                        label = "Escala izquierda",
                        valueText = String.format("%.2f", leftScale),
                        content = {
                            Slider(
                                value = leftScale,
                                onValueChange = { viewModel.updateLeftLens(
    scaleX = it,
    scaleY = lensConfig.leftScaleY,
    offsetX = lensConfig.leftOffsetX,
    offsetY = lensConfig.leftOffsetY
) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                )
                            )
                        }
                    )

                    VRControlRow(
                        label = "Escala derecha",
                        valueText = String.format("%.2f", rightScale),
                        content = {
                            Slider(
                                value = rightScale,
                                onValueChange = { viewModel.updateRightLens(
    scaleX = it,
    scaleY = lensConfig.rightScaleY,
    offsetX = lensConfig.rightOffsetX,
    offsetY = lensConfig.rightOffsetY
) },
                                valueRange = 0.5f..2.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                )
                            )
                        }
                    )

                    VRControlRowXY(
                        label = "Posición izquierda",
                        xValue = leftPosX,
                        yValue = leftPosY,
                        onXChange = { viewModel.updateLeftLens(
    lensConfig.leftScaleX,
    lensConfig.leftScaleY,
    it,
    lensConfig.leftOffsetY
) },
                        onYChange = { viewModel.updateLeftLens(
    lensConfig.leftScaleX,
    lensConfig.leftScaleY,
    lensConfig.leftOffsetX,
    it
) }
                    )

                    VRControlRowXY(
                        label = "Posición derecha",
                        xValue = rightPosX,
                        yValue = rightPosY,
                        onXChange = { viewModel.updateRightLens(
    lensConfig.rightScaleX,
    lensConfig.rightScaleY,
    it,
    lensConfig.rightOffsetY
) },
                        onYChange = { viewModel.updateRightLens(
    lensConfig.rightScaleX,
    lensConfig.rightScaleY,
    lensConfig.rightOffsetX,
    it
) }
                    )

                    VRControlRow(
                        label = "Distorsión óptica",
                        valueText = String.format("%.2f", lensCorrection),
                        content = {
                            Slider(
                                value = lensCorrection,
                                onValueChange = { viewModel.updateDistortion(it) },
                                valueRange = 0.5f..2f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF00FFCC),
                                    activeTrackColor = Color(0xFF00FFCC)
                                )
                            )
                        }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { viewModel.resetLensConfiguration() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A40)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("RESTABLECER", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    
                    Button(
                        onClick = { viewModel.saveLensConfiguration() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("GUARDAR Y APLICAR", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ConfigurablePanel(
    title: String,
    rotationY: Float,
    scaleX: Float,
    scaleY: Float,
    posX: Float,
    posY: Float,
    rotationZ: Float
) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .graphicsLayer {
    this.rotationY = rotationY
    this.scaleX = scaleX
    this.scaleY = scaleY
    this.translationX = posX * 100f
    this.translationY = posY * 100f
    this.rotationZ = rotationZ
}
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.5.dp, Color(0xFF6B4EFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun VRControlRow(label: String, valueText: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = valueText, color = Color(0xFF00FFCC), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        content()
    }
}

@Composable
fun VRControlRowXY(
    label: String,
    xValue: Float,
    yValue: Float,
    onXChange: (Float) -> Unit,
    onYChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "X",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.width(20.dp)
            )
            Slider(
                value = xValue,
                onValueChange = onXChange,
                valueRange = -0.5f..0.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FFCC),
                    activeTrackColor = Color(0xFF00FFCC)
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Y",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                modifier = Modifier.width(20.dp)
            )
            Slider(
                value = yValue,
                onValueChange = onYChange,
                valueRange = -0.5f..0.5f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFF00FFCC),
                    activeTrackColor = Color(0xFF00FFCC)
                )
            )
        }
    }
}