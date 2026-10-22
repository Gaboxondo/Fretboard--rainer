package com.threadboardlearn.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.threadboardlearn.model.GuitarString

@Composable
fun FretboardComposable(
    maxFret: Int = 12,
    targetString: GuitarString? = null,
    lastClickedFret: Pair<GuitarString, Int>? = null,
    isLastClickCorrect: Boolean? = null,
    onFretClicked: (GuitarString, Int) -> Unit
) {
    val strings = GuitarString.values() // E4, B3, G3, D3, A2, E2

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF261D17))
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(maxFret, strings) {
                    detectTapGestures { offset ->
                        val totalFrets = maxFret + 1 // Incluye traste 0 (cejuela)
                        val fretWidth = size.width / totalFrets
                        val clickedFret = (offset.x / fretWidth).toInt().coerceIn(0, maxFret)

                        val stringSpacing = size.height / (strings.size + 1)
                        var closestString = strings[0]
                        var minDistance = Float.MAX_VALUE

                        strings.forEachIndexed { index, guitarString ->
                            val stringY = stringSpacing * (index + 1)
                            val distance = kotlin.math.abs(offset.y - stringY)
                            if (distance < minDistance) {
                                minDistance = distance
                                closestString = guitarString
                            }
                        }

                        onFretClicked(closestString, clickedFret)
                    }
                }
        ) {
            val totalFrets = maxFret + 1
            val fretWidth = size.width / totalFrets
            val stringSpacing = size.height / (strings.size + 1)

            // 1. Dibujar Trastes (Fret Wires)
            for (fret in 0..maxFret) {
                val x = fret * fretWidth
                val wireColor = if (fret == 0) Color(0xFFD4C5A9) else Color(0xFF7A7A7A)
                val wireStroke = if (fret == 0) 8.dp.toPx() else 2.5.dp.toPx()
                drawLine(
                    color = wireColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = wireStroke
                )

                // Marcadores de posición (Inlay dots en trastes 3, 5, 7, 9, 12)
                if (fret in listOf(3, 5, 7, 9)) {
                    val dotX = (fret - 0.5f) * fretWidth
                    drawCircle(
                        color = Color(0x66FFFFFF),
                        radius = 4.dp.toPx(),
                        center = Offset(dotX, size.height / 2f)
                    )
                } else if (fret == 12) {
                    val dotX = (fret - 0.5f) * fretWidth
                    val offsetDistance = size.height * 0.22f
                    drawCircle(
                        color = Color(0x77FFFFFF),
                        radius = 3.5.dp.toPx(),
                        center = Offset(dotX, size.height / 2f - offsetDistance)
                    )
                    drawCircle(
                        color = Color(0x77FFFFFF),
                        radius = 3.5.dp.toPx(),
                        center = Offset(dotX, size.height / 2f + offsetDistance)
                    )
                }
            }

            // 2. Dibujar Cuerdas (Grosor variable según calibre)
            strings.forEachIndexed { index, guitarString ->
                val y = stringSpacing * (index + 1)
                val isTarget = guitarString == targetString
                val baseStroke = (1.2f + (index * 0.6f)).dp.toPx()

                // Resaltado sutil si es la cuerda objetivo del reto
                if (isTarget) {
                    drawLine(
                        color = Color(0x40FFD54F),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = baseStroke + 8.dp.toPx()
                    )
                }

                drawLine(
                    color = if (isTarget) Color(0xFFFFD54F) else Color(0xFFC8C8C8),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = baseStroke
                )
            }

            // 3. Feedback visual del último clic
            if (lastClickedFret != null && isLastClickCorrect != null) {
                val (clickedString, clickedFret) = lastClickedFret
                val stringIndex = strings.indexOf(clickedString)
                if (stringIndex != -1) {
                    val y = stringSpacing * (stringIndex + 1)
                    val x = if (clickedFret == 0) {
                        fretWidth * 0.35f
                    } else {
                        (clickedFret - 0.5f) * fretWidth
                    }

                    val feedbackColor = if (isLastClickCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                    drawCircle(
                        color = feedbackColor,
                        radius = 12.dp.toPx(),
                        center = Offset(x, y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
