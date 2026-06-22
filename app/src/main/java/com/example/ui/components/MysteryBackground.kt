package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center // ✅ إضافة الـ import الخاص بـ center
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.example.ui.theme.DarkBg
import androidx.compose.ui.graphics.graphicsLayer
private val BloodCenterGlow = Color(0xFF48100A)
private val BloodAmbientMid = Color(0xFF140302)
private val BloodPitchBlack = Color(0xFF090000)
private val CrimsonInk = Color(0xFF6E120A)
private val DarkBlood = Color(0xFF3B0703)

@Composable
fun MysteryBackground(
    modifier: Modifier = Modifier,
    drawBloodDrips: Boolean = false,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "BloodDripTransition")
    val dripAnimationFactor by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BloodProgress"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBg)
            .drawWithCache {
                // ✅ إصلاح: تحديد المركز بـ size.center ليعمل الـ Gradient بشكل صحيح و Responsive
                val radialBrush = Brush.radialGradient(
                    colors = listOf(BloodCenterGlow, BloodAmbientMid, BloodPitchBlack),
                    center = size.center, 
                    radius = size.maxDimension
                )

                onDrawBehind {
                    drawRect(brush = radialBrush)

                    if (drawBloodDrips) {
                        val path = Path()
                        val numDrops = 12
                        val dripWidth = size.width / numDrops.toFloat()
                        
                        val baseLineY = size.height * 0.03f 
                        val currentDripBaseY = baseLineY * (dripAnimationFactor * 0.4f + 0.8f)
                        
                        path.moveTo(0f, 0f)
                        path.lineTo(0f, currentDripBaseY)
                        
                        var currentX = 0f
                        for (i in 0..numDrops) {
                            val nextX = currentX + dripWidth
                            val midX = currentX + (dripWidth / 2f)
                            
                            val baseDropHeight = if (i % 3 == 0) {
                                size.height * 0.07f
                            } else if (i % 2 == 0) {
                                size.height * 0.05f
                            } else {
                                size.height * 0.035f
                            }
                            
                            val dynamicFactor = if (i % 2 == 0) dripAnimationFactor else (dripAnimationFactor * 0.9f + 0.1f)
                            val dropHeight = baseDropHeight * dynamicFactor
                            
                            path.cubicTo(
                                midX - (dripWidth / 4f), dropHeight + (size.height * 0.015f),
                                midX + (dripWidth / 4f), dropHeight + (size.height * 0.015f),
                                nextX, currentDripBaseY
                            )
                            currentX = nextX
                        }
                        path.lineTo(size.width, 0f)
                        path.close()
                        
                        val verticalBloodBrush = Brush.verticalGradient(
                            colors = listOf(DarkBlood, CrimsonInk),
                            startY = 0f,
                            endY = size.height * 0.08f * dripAnimationFactor
                        )
                        
                        drawPath(path = path, brush = verticalBloodBrush)
                    }
                }
            }
    ) {
        content()
    }
}