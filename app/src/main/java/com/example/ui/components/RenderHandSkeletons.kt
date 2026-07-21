package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.example.tracking.HandTracker.HandState
import kotlin.math.max


@Composable
fun RenderHandSkeletons(
    viewportW: Float,
    viewportH: Float,
    handState: HandState,
    style: String
) {

    if (!handState.isDetected) return


    val context = LocalContext.current

    val menuBitmap: ImageBitmap? = remember {

        try {

            val drawableId = context.resources.getIdentifier(
                "menu",
                "drawable",
                context.packageName
            )

            if (drawableId != 0) {

    val drawable = ContextCompat.getDrawable(
        context,
        drawableId
    )

    drawable?.toBitmap()?.asImageBitmap()

} else {
    null
}
        } catch (e: Exception) {
            null
        }
    }


    Canvas(
        modifier = Modifier.fillMaxSize()
    ) {


        handState.hands.forEach { hand ->


            if (hand.joints.size < 21)
                return@forEach


            val joints = hand.joints.map {

                Offset(
                    it.x * viewportW,
                    it.y * viewportH
                )

            }


            when(style){

                "skeleton" ->
                    drawSkeleton(
                        joints,
                        Color(0xFF00FFCC),
                        3f,
                        1f
                    )


                "transparent" ->
                    drawSkeleton(
                        joints,
                        Color(0xFF00FFCC),
                        2f,
                        0.5f
                    )


                "solid" ->
                    drawSolidHand(
                        joints,
                        Color(0xFF00FFCC),
                        0.9f
                    )


                "holographic" ->
                    drawHolographicHand(
                        joints,
                        Color(0xFF00FFCC)
                    )


                else ->
                    drawSkeleton(
                        joints,
                        Color(0xFF00FFCC),
                        3f,
                        1f
                    )
            }



            if(hand.menuVisible && menuBitmap != null){


                val anchorX =
                    hand.menuAnchorX * viewportW

                val anchorY =
                    hand.menuAnchorY * viewportH



                val imageW =
                    menuBitmap.width.toFloat()

                val imageH =
                    menuBitmap.height.toFloat()



                val size =
                    minOf(
                        imageW,
                        imageH,
                        100f
                    )


                val scale =
                    size / max(
                        imageW,
                        imageH
                    )


                val width =
                    imageW * scale


                val height =
                    imageH * scale



                val x =
                    anchorX - width / 2f


                val y =
                    anchorY - height / 2f



                // Glow
                drawCircle(
                    color = Color(0xFF0066FF)
                        .copy(alpha = 0.3f),

                    radius = size / 2f + 8f,

                    center = Offset(
                        anchorX,
                        anchorY
                    )
                )



                // Shadow
                drawImage(

                    image = menuBitmap,

                    dstOffset = IntOffset(
                        (x + 2f).toInt(),
                        (y + 2f).toInt()
                    ),

                    dstSize = IntSize(
                        width.toInt(),
                        height.toInt()
                    ),

                    alpha = 0.25f
                )



                // Icon
                drawImage(

                    image = menuBitmap,

                    dstOffset = IntOffset(
                        x.toInt(),
                        y.toInt()
                    ),

                    dstSize = IntSize(
                        width.toInt(),
                        height.toInt()
                    ),

                    alpha = 1f
                )
            }

        }

    }
}



private fun DrawScope.drawSkeleton(
    joints: List<Offset>,
    color: Color,
    width: Float,
    alpha: Float
){


    val connections = listOf(

        0 to 1,
        1 to 2,
        2 to 3,
        3 to 4,

        0 to 5,
        5 to 6,
        6 to 7,
        7 to 8,

        5 to 9,
        9 to 10,
        10 to 11,
        11 to 12,

        9 to 13,
        13 to 14,
        14 to 15,
        15 to 16,

        13 to 17,
        17 to 18,
        18 to 19,
        19 to 20,

        0 to 17
    )


    connections.forEach { (a,b)->

        drawLine(

            color.copy(alpha = alpha),

            joints[a],
            joints[b],

            strokeWidth = width,

            cap = StrokeCap.Round
        )
    }



    joints.forEach {

        drawCircle(

            color.copy(alpha = alpha),

            radius = width * 1.5f,

            center = it
        )
    }

}



private fun DrawScope.drawSolidHand(
    joints: List<Offset>,
    color: Color,
    alpha: Float
){

    val tips =
        listOf(
            4,
            8,
            12,
            16,
            20
        ).map {
            joints[it]
        }



    val palm =
        joints[0]


    val path =
        Path().apply {

            moveTo(
                palm.x,
                palm.y
            )


            tips.forEach {

                lineTo(
                    it.x,
                    it.y
                )

            }

            close()
        }



    drawPath(
        path,
        color.copy(
            alpha = alpha * 0.3f
        )
    )


    tips.forEach {

        drawCircle(

            color.copy(alpha = alpha),

            radius = 8f,

            center = it
        )

    }

}



private fun DrawScope.drawHolographicHand(
    joints: List<Offset>,
    color: Color
){


    drawSkeleton(
        joints,
        color,
        8f,
        0.25f
    )


    drawSkeleton(
        joints,
        color,
        2f,
        0.9f
    )


    joints.forEach {


        drawCircle(

            color.copy(alpha = 0.25f),

            radius = 10f,

            center = it
        )


        drawCircle(

            color,

            radius = 3f,

            center = it
        )

    }

}