package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.example.tracking.HandTracker.HandState

@Composable
fun RenderHandSkeletons(
    viewportW: Float,
    viewportH: Float,
    handState: HandState,
    style: String
) {
    if (!handState.isDetected) return

    Canvas(modifier = Modifier.fillMaxSize()) {
        handState.hands.forEach { hand ->
            if (hand.joints.size < 21) return@forEach
            
            // Las coordenadas de Joint están en espacio normalizado de imagen (0..1)
            // Para dibujarlas correctamente en pantalla, las multiplicamos por el viewport
            val joints = hand.joints.map { 
                Offset(it.x * viewportW, it.y * viewportH) 
            }
            
            when (style) {
                "skeleton" -> drawSkeleton(joints, Color(0xFF00FFCC), 3f, alpha = 1f)
                "transparent" -> drawSkeleton(joints, Color(0xFF00FFCC), 2f, alpha = 0.5f)
                "solid" -> drawSolidHand(joints, Color(0xFF00FFCC), alpha = 0.9f)
                "holographic" -> drawHolographicHand(joints, Color(0xFF00FFCC))
                else -> drawSkeleton(joints, Color(0xFF00FFCC), 3f, alpha = 1f)
            }
        }
    }
}

private fun DrawScope.drawSkeleton(joints: List<Offset>, color: Color, width: Float, alpha: Float) {
    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,         // pulgar
        0 to 5, 5 to 6, 6 to 7, 7 to 8,         // índice
        5 to 9, 9 to 10, 10 to 11, 11 to 12,    // medio
        9 to 13, 13 to 14, 14 to 15, 15 to 16,  // anular
        13 to 17, 17 to 18, 18 to 19, 19 to 20, // meñique
        0 to 17                                  // palma
    )
    connections.forEach { (a, b) ->
        drawLine(color.copy(alpha = alpha), joints[a], joints[b], strokeWidth = width, cap = StrokeCap.Round)
    }
    joints.forEach { p ->
        drawCircle(color.copy(alpha = alpha), radius = width * 1.5f, center = p)
    }
}

private fun DrawScope.drawSolidHand(joints: List<Offset>, color: Color, alpha: Float) {
    val tips = listOf(4, 8, 12, 16, 20).map { joints[it] }
    val palm = joints[0]
    val path = Path().apply {
        moveTo(palm.x, palm.y)
        tips.forEach { lineTo(it.x, it.y) }
        close()
    }
    drawPath(path, color.copy(alpha = alpha * 0.3f))
    tips.forEach { drawCircle(color.copy(alpha = alpha), radius = 8f, center = it) }
}

private fun DrawScope.drawHolographicHand(joints: List<Offset>, color: Color) {
    val connections = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        5 to 9, 9 to 10, 10 to 11, 11 to 12,
        9 to 13, 13 to 14, 14 to 15, 15 to 16,
        13 to 17, 17 to 18, 18 to 19, 19 to 20,
        0 to 17
    )
    connections.forEach { (a, b) ->
        drawLine(color.copy(alpha = 0.25f), joints[a], joints[b], strokeWidth = 8f, cap = StrokeCap.Round)
        drawLine(color.copy(alpha = 0.9f), joints[a], joints[b], strokeWidth = 2f, cap = StrokeCap.Round)
    }
    joints.forEach { p ->
        drawCircle(color.copy(alpha = 0.25f), radius = 10f, center = p)
        drawCircle(color.copy(alpha = 1f), radius = 3f, center = p)
    }
}