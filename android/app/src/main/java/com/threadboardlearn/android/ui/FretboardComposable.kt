package com.threadboardlearn.android.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.threadboardlearn.model.GuitarString

@Composable
fun FretboardComposable(
    modifier: Modifier = Modifier,
    maxFret: Int = 12,
    targetString: GuitarString? = null,
    lastClickedFret: Pair<GuitarString, Int>? = null,
    isLastClickCorrect: Boolean? = null,
    solutionFrets: List<Int>? = null,
    onFretClicked: (GuitarString, Int) -> Unit
) {
    val strings = GuitarString.values() // E4, B3, G3, D3, A2, E2
    val nutWidthDp = 58.dp

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 1. Barra de Números de Traste perfectamente alineada
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.width(nutWidthDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0 / Aire",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFD4C5A9),
                    textAlign = TextAlign.Center
                )
            }

            for (fret in 1..maxFret) {
                val isMarked = fret in listOf(3, 5, 7, 9, 12)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$fret",
                        fontSize = if (isMarked) 12.sp else 11.sp,
                        fontWeight = if (isMarked) FontWeight.Black else FontWeight.Medium,
                        color = if (isMarked) Color(0xFFFFD54F) else Color(0xFF888888),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 2. Mástil y Cuerdas interactivos
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
                .heightIn(min = 160.dp, max = 240.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF221914))
                .border(1.dp, Color(0xFF4A382A), RoundedCornerShape(12.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(maxFret, strings) {
                        detectTapGestures { offset ->
                            val nutWidthPx = nutWidthDp.toPx()
                            val playableWidth = size.width - nutWidthPx
                            val fretWidth = playableWidth / maxFret

                            // 1. Detección exacta de Traste (sin desplazamiento de -1)
                            val clickedFret = if (offset.x < nutWidthPx) {
                                0 // Cuerda al aire
                            } else {
                                val f = 1 + ((offset.x - nutWidthPx) / fretWidth).toInt()
                                f.coerceIn(1, maxFret)
                            }

                            // 2. Detección exacta de Cuerda
                            val topPadding = 18.dp.toPx()
                            val bottomPadding = 20.dp.toPx()
                            val usableHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
                            val stringSpacing = usableHeight / 5.0f

                            var closestString = strings[0]
                            var minDistance = Float.MAX_VALUE

                            strings.forEachIndexed { index, guitarString ->
                                val stringY = topPadding + index * stringSpacing
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
                val nutWidthPx = nutWidthDp.toPx()
                val playableWidth = size.width - nutWidthPx
                val fretWidth = playableWidth / maxFret

                val topPadding = 18.dp.toPx()
                val bottomPadding = 20.dp.toPx()
                val usableHeight = (size.height - topPadding - bottomPadding).coerceAtLeast(1f)
                val stringSpacing = usableHeight / 5.0f

                // A. Fondo diferenciado para Cejuela / Traste 0 (cuerda al aire)
                drawRect(
                    color = Color(0xFF1B140F),
                    topLeft = Offset(0f, 0f),
                    size = Size(nutWidthPx, size.height)
                )

                // B. Barra de Cejuela (Nut de hueso/marfil)
                val nutBarWidth = 7.dp.toPx()
                drawRect(
                    color = Color(0xFFDCD2BE),
                    topLeft = Offset(nutWidthPx - nutBarWidth, 0f),
                    size = Size(nutBarWidth, size.height)
                )
                // Sombra Cejuela
                drawLine(
                    color = Color(0x66000000),
                    start = Offset(nutWidthPx, 0f),
                    end = Offset(nutWidthPx, size.height),
                    strokeWidth = 2.dp.toPx()
                )

                // C. Marcadores de posición (Inlay dots de nácar en trastes 3, 5, 7, 9, 12)
                val inlays = listOf(3, 5, 7, 9)
                inlays.forEach { fret ->
                    if (fret <= maxFret) {
                        val dotX = nutWidthPx + (fret - 0.5f) * fretWidth
                        val centerY = topPadding + 2.5f * stringSpacing
                        drawCircle(
                            color = Color(0x55FFFFFF),
                            radius = 5.dp.toPx(),
                            center = Offset(dotX, centerY)
                        )
                    }
                }
                if (12 <= maxFret) {
                    val dotX = nutWidthPx + (12 - 0.5f) * fretWidth
                    val dotY1 = topPadding + 1.2f * stringSpacing
                    val dotY2 = topPadding + 3.8f * stringSpacing
                    drawCircle(
                        color = Color(0x66FFFFFF),
                        radius = 4.5.dp.toPx(),
                        center = Offset(dotX, dotY1)
                    )
                    drawCircle(
                        color = Color(0x66FFFFFF),
                        radius = 4.5.dp.toPx(),
                        center = Offset(dotX, dotY2)
                    )
                }

                // D. Trastes metálicos (Fret Wires 1 a maxFret)
                for (fret in 1..maxFret) {
                    val wireX = nutWidthPx + fret * fretWidth
                    // Sombra metálica
                    drawLine(
                        color = Color(0x88000000),
                        start = Offset(wireX + 1.dp.toPx(), 0f),
                        end = Offset(wireX + 1.dp.toPx(), size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                    // Alambre metálico brillante
                    drawLine(
                        color = Color(0xFF90939A),
                        start = Offset(wireX, 0f),
                        end = Offset(wireX, size.height),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                // E. Resaltar Cuerda Objetivo (Target String) si hay un reto activo
                strings.forEachIndexed { index, guitarString ->
                    val y = topPadding + index * stringSpacing
                    val isTarget = (guitarString == targetString)
                    if (isTarget) {
                        drawRoundRect(
                            color = Color(0x35FFD54F),
                            topLeft = Offset(0f, y - (stringSpacing * 0.42f)),
                            size = Size(size.width, stringSpacing * 0.84f),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                        )
                    }
                }

                // F. Dibujar las 6 Cuerdas con grosor y textura realista
                strings.forEachIndexed { index, guitarString ->
                    val y = topPadding + index * stringSpacing
                    val isTarget = (guitarString == targetString)
                    val strokeWidth = (1.4f + (index * 0.65f)).dp.toPx()

                    // Sombra de la cuerda
                    drawLine(
                        color = Color(0x99000000),
                        start = Offset(0f, y + 1.5.dp.toPx()),
                        end = Offset(size.width, y + 1.5.dp.toPx()),
                        strokeWidth = strokeWidth
                    )

                    // Cuerda
                    val stringColor = when {
                        isTarget -> Color(0xFFFFD54F) // Oro brillante para la cuerda del reto
                        index >= 2 -> Color(0xFFD2C7B2) // Entorchadas (bronce/níquel)
                        else -> Color(0xFFE0E2EC)       // Lisas (acero plateado)
                    }

                    drawLine(
                        color = stringColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )

                    // Punto guía de cuerda en la cejuela
                    drawCircle(
                        color = if (isTarget) Color(0xFFFFD54F) else Color(0x55888888),
                        radius = 6.dp.toPx(),
                        center = Offset(nutWidthPx * 0.45f, y)
                    )
                }

                // G. Solución mostrada en caso de Timeout
                if (targetString != null && !solutionFrets.isNullOrEmpty()) {
                    val targetIndex = strings.indexOf(targetString)
                    if (targetIndex != -1) {
                        val y = topPadding + targetIndex * stringSpacing
                        solutionFrets.forEach { solFret ->
                            val x = if (solFret == 0) {
                                nutWidthPx * 0.5f
                            } else {
                                nutWidthPx + (solFret - 0.5f) * fretWidth
                            }

                            // Halo dorado de solución
                            drawCircle(
                                color = Color(0x66FFD54F),
                                radius = 18.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color(0xFFFFD54F),
                                radius = 10.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.Black,
                                radius = 4.dp.toPx(),
                                center = Offset(x, y)
                            )
                        }
                    }
                }

                // H. Feedback visual del toque del usuario (Verde acierto / Rojo fallo)
                if (lastClickedFret != null && isLastClickCorrect != null) {
                    val (clickedString, clickedFret) = lastClickedFret
                    val stringIndex = strings.indexOf(clickedString)
                    if (stringIndex != -1) {
                        val y = topPadding + stringIndex * stringSpacing
                        val x = if (clickedFret == 0) {
                            nutWidthPx * 0.5f
                        } else {
                            nutWidthPx + (clickedFret - 0.5f) * fretWidth
                        }

                        val feedbackColor = if (isLastClickCorrect) Color(0xFF4CAF50) else Color(0xFFF44336)
                        drawCircle(
                            color = feedbackColor,
                            radius = 14.dp.toPx(),
                            center = Offset(x, y)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 5.dp.toPx(),
                            center = Offset(x, y)
                        )
                    }
                }
            }
        }
    }
}
