package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.VRViewModel
import com.example.ui.theme.MyApplicationTheme

class VRConfigActivity : ComponentActivity() {
    private lateinit var vrViewModel: VRViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                vrViewModel = viewModel()
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
                        VRConfigScreen()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::vrViewModel.isInitialized) {
            vrViewModel.startTracking()
        }
    }

    override fun onPause() {
        if (::vrViewModel.isInitialized) {
            vrViewModel.stopTracking()
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}

@Composable
fun VRConfigScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0C0724)), // Fondo oscuro inmersivo
        contentAlignment = Alignment.Center
    ) {
        // Panel flotante en espacio 3D: tamaño reducido, distancia cómoda
        GlassPanel(
            modifier = Modifier
                .width(480.dp) // Ancho cómodo para lectura en VR
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

                // Cuadros/lentes configurados visibles completamente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ConfigurablePanel(title = "Lente Izquierdo", rotationY = 5f)
                    ConfigurablePanel(title = "Lente Derecho", rotationY = -5f)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { /* Guardar configuración */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00FFCC)),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("GUARDAR Y APLICAR", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ConfigurablePanel(title: String, rotationY: Float) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .graphicsLayer {
                this.rotationY = rotationY // Efecto 3D sutil
                this.rotationY = rotationY // Efecto 3D sutil    // Elevación para que no se pierda en el fondo
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .border(1.5.dp, Color(0xFF6B4EFF), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}