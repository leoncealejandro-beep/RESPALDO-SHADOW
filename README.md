# 🥽 ShadowVR

<p align="center">
  <img src="docs/logo.png" width="180"/>
</p>

<h3 align="center">
Un sistema VR experimental para convertir dispositivos Android en una plataforma de realidad virtual completa.
</h3>

<p align="center">
Streaming inalámbrico • Passthrough • Hand Tracking • Menús espaciales • PC VR • OpenXR
</p>

---

# 📖 Sobre ShadowVR

ShadowVR es un proyecto experimental de realidad virtual cuyo objetivo es crear una plataforma VR ligera, modular y abierta utilizando dispositivos Android.

La idea principal es transformar un teléfono o tablet Android en un visor VR capaz de:

- Recibir contenido desde un PC por WiFi con baja latencia.
- Mostrar escritorios y aplicaciones en espacios 3D.
- Usar seguimiento de cabeza.
- Controlar interfaces mediante las manos.
- Crear una experiencia similar a sistemas como Meta Quest, pero utilizando hardware más accesible.

ShadowVR busca crear una alternativa abierta y personalizable para experimentar con VR sin depender de ecosistemas cerrados.

---

# 🎯 Objetivo del proyecto

El objetivo final de ShadowVR es construir un sistema VR completo:

             PC
              |
    ┌─────────────────┐
    │ ShadowVR Server │
    └─────────────────┘
              |
    Video / Audio / Input
              |
            WiFi
              |
    ┌─────────────────┐
    │ ShadowVR Client │
    │ Android Device  │
    └─────────────────┘
              |
          VR Display

El usuario puede utilizar un teléfono Android dentro de un visor Cardboard o similar para tener:

- Pantalla gigante virtual.
- Escritorio VR.
- Aplicaciones flotantes.
- Juegos PC.
- Control mediante manos.

---

# ✨ Características planeadas

## 🖥️ Streaming desde PC

Sistema de transmisión optimizado para enviar contenido del ordenador al dispositivo Android.

Características:

- Captura de pantalla usando APIs modernas.
- Baja latencia.
- Codificación por hardware.
- Adaptación automática de calidad.

Códecs planeados:

- H264
- H265 / HEVC
- AV1

Aceleradores compatibles:

- NVIDIA NVENC
- AMD AMF
- Intel Quick Sync

---

# 🥽 Renderizado VR

ShadowVR utiliza un sistema de renderizado estéreo para dispositivos VR.

Características:

- Vista izquierda/derecha.
- Corrección de lente.
- Ajuste IPD.
- Campo de visión configurable.
- Seguimiento de cabeza.

Compatible con:

- Google Cardboard.
- OpenXR (planeado).
- Dispositivos Android VR.

---

# 👋 Control mediante manos

Una de las funciones principales de ShadowVR es controlar la interfaz sin controles físicos.

Utiliza:

- Cámara del dispositivo.
- MediaPipe Hand Tracking.
- Reconocimiento de gestos.

Gestos planeados:

| Gesto | Acción |
|-|-|
| Pinch | Seleccionar |
| Mano abierta | Abrir menú |
| Movimiento dedo índice | Cursor VR |
| Dos manos | Escalado de ventanas |

---

# 🌌 Sistema de ventanas VR

ShadowVR intenta crear un entorno parecido a un sistema operativo VR.

Incluye:

- Ventanas flotantes.
- Movimiento en espacio 3D.
- Escalado.
- Rotación.
- Organización espacial.

Ejemplo:

      Navegador

          🖥️

Juego Chat

          🎮

      Usuario VR

---

# 🧩 Arquitectura del proyecto

## Android Client

Aplicación principal encargada de:

- Render VR.
- Cámara.
- Tracking.
- UI espacial.
- Entrada del usuario.

Tecnologías:

- Kotlin
- Jetpack Compose
- Android SDK
- OpenGL / Vulkan
- MediaPipe

---

## ShadowVR Android Plugin

Plugin nativo para funciones avanzadas:


ShadowVR-Android

android/plugins/

└── mediapipe_plugin

  ├── bridge
  │    └── GodotBridge.kt
  │
  ├── camera
  │    └── CameraManager.kt
  │
  ├── tracking
  │
  ├── render
  │
  └── utils

Responsabilidades:

- Cámara.
- Tracking de manos.
- Comunicación con motor VR.

---

## Base de datos local

ShadowVR utiliza almacenamiento local para guardar configuraciones:

Datos guardados:

- Ventanas VR.
- Posiciones.
- Preferencias.
- Ajustes gráficos.
- Configuración del usuario.

---

# 🛠️ Tecnologías utilizadas

## Lenguajes

- Kotlin
- Java
- C++
- GDScript

---

## Android

- Android Studio
- Gradle
- Jetpack Compose
- CameraX

---

## Computer Vision

- MediaPipe Tasks Vision
- OpenCV

---

## VR

- OpenXR
- Google Cardboard
- OpenGL ES
- Vulkan (futuro)

---

# 📦 Instalación

## Requisitos

### PC

- Windows 10/11
- GPU compatible
- WiFi 5 o superior recomendado
- Android Studio

---

### Android

Recomendado:

- Android 10+
- Cámara frontal o trasera funcional
- Sensor de movimiento

---

# 🚀 Compilar ShadowVR

Clonar el repositorio:

```bash
git clone https://github.com/leoncealejandro-beep/RESPALDO-SHADOW

cd ShadowVR

Abrir con Android Studio:

File
 ↓
Open
 ↓
ShadowVR

Esperar sincronización de Gradle.

Compilar:

gradlew assembleDebug

Instalar:

adb install app-debug.apk
🧪 Cómo probar ShadowVR
Prueba básica
Instalar APK.
Abrir ShadowVR.
Permitir permisos:
Cámara.
Sensores.
Colocar dispositivo en visor VR.
Revisar renderizado estéreo.
Prueba de seguimiento de manos
Activar:
Configuración
 └── Hand Tracking
Colocar la mano frente a la cámara.
Verificar:
Detección de dedos.
Cursor.
Gestos.
Prueba del menú VR

Abrir:

Menú principal
       |
       └── Entorno VR

Comprobar:

Aparición del dock.
Movimiento de ventanas.
Selección con manos.
🐛 Solución de problemas
Error:
SDK location not found

Solución:

Crear:

local.properties

Agregar:

sdk.dir=C:\\Users\\Usuario\\AppData\\Local\\Android\\Sdk
Error:
JAVA_HOME not set

Configurar:

Windows:

JAVA_HOME=
ruta_del_JDK
Tracking no funciona

Revisar:

Permiso de cámara.
Buena iluminación.
Cámara limpia.
Distancia correcta de la mano.
📈 Estado actual
Implementado

✅ Aplicación Android base
✅ Menú principal VR
✅ Sistema de configuración
✅ Render VR experimental
✅ Sistema de ventanas
✅ Seguimiento de cabeza
✅ Integración inicial MediaPipe
✅ Menú controlado con manos (en desarrollo)

🚧 En desarrollo

Actualmente:

Mejorando interacción de manos.
Mejorando estabilidad del render.
Optimización de latencia.
Comunicación PC ↔ Android.
Sistema de streaming.
🗺️ Roadmap
Fase 1 - Fundación

✅ Aplicación VR Android
✅ UI base
✅ Render estéreo

Fase 2 - Interacción

🔄 Hand Tracking

🔄 Gestos avanzados

🔄 Cursor VR

Fase 3 - Streaming

⬜ Captura PC

⬜ Encoder hardware

⬜ Streaming WiFi

Fase 4 - Sistema VR completo

⬜ Escritorio virtual

⬜ Aplicaciones flotantes

⬜ Compatibilidad OpenXR

🤝 Contribuir

ShadowVR es un proyecto experimental.

Las contribuciones son bienvenidas:

Mejoras de rendimiento.
Nuevos sistemas VR.
Correcciones.
Ideas.

Proceso:

Fork del proyecto.
Crear rama:
git checkout -b nueva-funcion
Realizar cambios.
Crear Pull Request.
📜 Licencia

Proyecto experimental creado para investigación y aprendizaje.

La licencia final será definida cuando el proyecto alcance una versión estable.

👤 Autor

leoale

Proyecto creado como experimento para explorar:

Realidad virtual.
Computer Vision.
Android avanzado.
Interfaces espaciales.
⭐ Apoya el proyecto

Si ShadowVR te parece interesante:

⭐ Dale una estrella al repositorio.