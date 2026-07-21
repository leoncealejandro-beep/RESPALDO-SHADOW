package com.example.ui.apps

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ui.VRViewModel
import com.example.ui.apps.calculator.CalculatorApp

/**
 * Metadata + contenido composable de una app nativa registrada.
 */
data class NativeApp(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val desc: String,
    val category: String,
    val content: @Composable (windowId: String, viewModel: VRViewModel) -> Unit
)

/**
 * Registro global de apps nativas.
 * El Launcher y VRWindowContent lo consultan para resolver IDs dinámicamente.
 */
object AppRegistry {

    private val _apps = linkedMapOf<String, NativeApp>()
    val apps: Map<String, NativeApp> get() = _apps

    fun register(app: NativeApp) {
        _apps[app.id] = app
    }

    fun getApp(id: String): NativeApp? = _apps[id.trim()]

    fun getAllApps(): List<NativeApp> = _apps.values.toList()

    /**
     * Se llama una sola vez al arrancar la app (Application.onCreate).
     */
    fun registerAll() {
        register(CalculatorApp.definition)
        // register(NotesApp.definition)
        // register(ClockApp.definition)
    }
}