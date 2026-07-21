package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.DarkBg

@Composable
fun MysteryBackground(
    modifier: Modifier = Modifier,
    drawBloodDrips: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val bloodPath = remember { Path() }
    val radialBrush = remember {
        Brush.radialGradient(
            colors = listOf(
                Color(0xFF48100A),
                Color(0xFF140302),
                Color(0xFF090000)
            ),
            center = Offset.Unspecified,
            radius = 1800f
        )
    }

    val bloodGradient = remember {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF3B0703), Color(0xFF6E120A)),
            startY = 0f,
            endY = 160f
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .background(brush = radialBrush)
            .drawBehind {
                if (drawBloodDrips) {
                    bloodPath.reset()
                    bloodPath.moveTo(0f, 0f)

                    var currentX = 0f
                    val dripWidth = size.width / 12f
                    bloodPath.lineTo(0f, 60f)

                    for (i in 0 until 12) {
                        val nextX = currentX + dripWidth
                        val midX = currentX + (dripWidth / 2f)
                        val dropHeight = if (i % 3 == 0) 140f else if (i % 2 == 0) 100f else 70f

                        bloodPath.cubicTo(
                            midX - (dripWidth / 4f), dropHeight + 30f,
                            midX + (dripWidth / 4f), dropHeight + 30f,
                            nextX, 60f
                        )
                        currentX = nextX
                    }
                    bloodPath.lineTo(size.width, 0f)
                    bloodPath.close()

                    drawPath(
                        path = bloodPath,
                        brush = bloodGradient
                    )
                }
            }
    ) {
        content()
    }
}