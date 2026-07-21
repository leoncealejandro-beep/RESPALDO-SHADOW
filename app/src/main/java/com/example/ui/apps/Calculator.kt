package com.example.ui.apps.calculator

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.VRViewModel
import com.example.ui.apps.NativeApp

object CalculatorApp {

    val definition = NativeApp(
        id = "calculator",
        name = "Calculadora",
        icon = Icons.Default.Calculate,
        desc = "Cálculos rápidos espaciales",
        category = "Herramientas",
        content = { windowId, viewModel ->
            CalculatorContent(windowId, viewModel)
        }
    )
}


@Composable
fun CalculatorContent(
    windowId: String,
    viewModel: VRViewModel
) {

    var display by remember { mutableStateOf("0") }
    var accumulator by remember { mutableStateOf(0.0) }
    var pendingOp by remember { mutableStateOf<String?>(null) }
    var resetOnNextDigit by remember { mutableStateOf(false) }


    val Accent = Color(0xFF00FFCC)
    val GlassFill = Color.White.copy(alpha = 0.08f)


    fun inputDigit(d: String) {

        display =
            if (display == "0" || resetOnNextDigit) {
                resetOnNextDigit = false
                d
            } else {
                display + d
            }
    }


    fun inputDot() {

        if (resetOnNextDigit) {

            display = "0."
            resetOnNextDigit = false

        } else if (!display.contains(".")) {

            display += "."
        }
    }


    fun compute(
        a: Double,
        b: Double,
        op: String
    ): Double {

        return when(op) {

            "+" -> a + b

            "−" -> a - b

            "×" -> a * b

            "÷" ->
                if (b == 0.0)
                    Double.NaN
                else
                    a / b

            else -> b
        }
    }


    fun formatResult(
        value: Double
    ): String {

        return when {

            value.isNaN() ->
                "Error"

            value == value.toLong().toDouble() ->
                value.toLong().toString()

            else ->
                String.format("%.6f", value)
                    .trimEnd('0')
                    .trimEnd('.')
        }
    }


    fun applyOp(op: String) {

        val current =
            display.toDoubleOrNull() ?: 0.0


        if (pendingOp == null) {

            accumulator = current

        } else if (!resetOnNextDigit) {

            accumulator =
                compute(
                    accumulator,
                    current,
                    pendingOp!!
                )

            display =
                formatResult(accumulator)
        }


        pendingOp = op
        resetOnNextDigit = true
    }


    fun doEquals() {

        val current =
            display.toDoubleOrNull() ?: 0.0


        if (pendingOp != null) {

            accumulator =
                compute(
                    accumulator,
                    current,
                    pendingOp!!
                )

            display =
                formatResult(accumulator)

            pendingOp = null
        }


        resetOnNextDigit = true
    }


    fun clear() {

        display = "0"
        accumulator = 0.0
        pendingOp = null
        resetOnNextDigit = false
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {


        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                Icons.Default.Calculate,
                null,
                tint = Accent,
                modifier = Modifier.size(18.dp)
            )


            Spacer(
                Modifier.size(6.dp)
            )


            Text(
                "Calculadora VR",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )


            Spacer(
                Modifier.weight(1f)
            )


            Text(
                pendingOp ?: "—",
                fontSize = 10.sp,
                color = Accent,
                fontWeight = FontWeight.Bold
            )
        }



        Spacer(
            Modifier.height(10.dp)
        )



        Card(
            colors =
                CardDefaults.cardColors(
                    containerColor =
                        Color.Black.copy(alpha = 0.4f)
                ),
            shape =
                RoundedCornerShape(12.dp),
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(72.dp)
        ) {


            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),

                contentAlignment =
                    Alignment.CenterEnd
            ) {


                Text(
                    display,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }



        Spacer(
            Modifier.height(10.dp)
        )



        val rows =
            listOf(

                listOf("C","±","%","÷"),

                listOf("7","8","9","×"),

                listOf("4","5","6","−"),

                listOf("1","2","3","+"),

                listOf("0",".","=")
            )



        rows.forEach { row ->


            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                horizontalArrangement =
                    Arrangement.spacedBy(6.dp)
            ) {


                row.forEach { key ->


                    val isOp =
                        key in listOf(
                            "+",
                            "−",
                            "×",
                            "÷",
                            "="
                        )


                    val isFn =
                        key in listOf(
                            "C",
                            "±",
                            "%"
                        )


                    val isZero =
                        key == "0"



                    val bg =
                        when {

                            key == "=" ->
                                Accent

                            isOp ->
                                Accent.copy(alpha = 0.25f)

                            isFn ->
                                Color.White.copy(alpha = 0.15f)

                            else ->
                                GlassFill
                        }



                    val fg =
                        when {

                            key == "=" ->
                                Color.Black

                            isOp ->
                                Accent

                            else ->
                                Color.White
                        }



                    Card(

                        colors =
                            CardDefaults.cardColors(
                                containerColor = bg
                            ),

                        shape =
                            RoundedCornerShape(10.dp),

                        modifier =
                            Modifier
                                .weight(
                                    if(isZero)
                                        2.1f
                                    else
                                        1f
                                )
                                .height(44.dp)

                                .clickable {


                                    when(key) {


                                        "C" ->
                                            clear()


                                        "±" ->

                                            display =
                                                formatResult(
                                                    -(display.toDoubleOrNull() ?: 0.0)
                                                )


                                        "%" ->

                                            display =
                                                formatResult(
                                                    (display.toDoubleOrNull() ?: 0.0) / 100
                                                )


                                        "=" ->
                                            doEquals()


                                        "." ->
                                            inputDot()


                                        "+",
                                        "−",
                                        "×",
                                        "÷" ->
                                            applyOp(key)


                                        else ->
                                            inputDigit(key)
                                    }
                                }

                    ) {


                        Box(
                            Modifier.fillMaxSize(),
                            contentAlignment =
                                Alignment.Center
                        ) {


                            Text(

                                key,

                                fontSize = 16.sp,

                                fontWeight =
                                    FontWeight.Bold,

                                color = fg,

                                textAlign =
                                    TextAlign.Center
                            )
                        }
                    }
                }
            }


            Spacer(
                Modifier.height(6.dp)
            )
        }
    }
}