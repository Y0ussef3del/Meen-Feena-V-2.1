package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.random.Random

/**
 * دالة إنشاء شكل الورقة ذات الحواف المتعرجة مع إعطاء أداء عالي وعالي السلاسة
 */
fun createTornPaperShape(seed: Long = 42L): GenericShape {
    val rand = Random(seed)
    val numPoints = 60

    val topOffsets = FloatArray(numPoints + 1) { i -> if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f) }
    val rightOffsets = FloatArray(numPoints + 1) { i -> if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f) }
    val bottomOffsets = FloatArray(numPoints + 1) { i -> if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f) }
    val leftOffsets = FloatArray(numPoints + 1) { i -> if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f) }

    return GenericShape { size, _ ->
        moveTo(0f, 0f)
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = topOffsets[i]
            lineTo(x, y)
        }
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = size.width + rightOffsets[i]
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = size.height + bottomOffsets[i]
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = leftOffsets[i]
            lineTo(x, y)
        }
        close()
    }
}

@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    seed: Long = 100L,
    elevation: Dp = 6.dp,
    contentPadding: PaddingValues = PaddingValues(scaledDp(16)),
    isAnimated: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    val responsiveElevation = scaledDp(elevation.value.toInt())

    val unfoldProgressAnim = remember { Animatable(0f) }

    LaunchedEffect(isAnimated) {
        if (isAnimated) {
            unfoldProgressAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
            )
        } else {
            unfoldProgressAnim.snapTo(1f)
        }
    }

    val inset = scaledDp(12).value
    val strokeWidth = scaledDp(2).value

    Box(
        modifier = modifier
            .graphicsLayer {
                val progress = unfoldProgressAnim.value

                scaleY = 0.01f + (progress * 0.99f)
                rotationX = (1f - progress) * -45f
                alpha = (progress * 1.5f).coerceIn(0f, 1f)

                transformOrigin = TransformOrigin(0.5f, 0f)
            }
            .shadow(
                elevation = responsiveElevation,
                shape = tornShape,
                clip = false,
                ambientColor = Color.Black,
                spotColor = Color(0xFF1E140B)
            )
            .clip(tornShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(PapyrusBgLight, PapyrusBg),
                    center = Offset.Unspecified
                )
            )
            .drawBehind {
                val progress = unfoldProgressAnim.value

                if (progress < 0.98f) {
                    val creaseAlpha = (1f - progress) * 0.3f
                    drawLine(
                        color = Color(0xFF2B1A0A).copy(alpha = creaseAlpha),
                        start = Offset(0f, size.height * 0.5f),
                        end = Offset(size.width, size.height * 0.52f),
                        strokeWidth = 4f
                    )
                } else {
                    drawLine(
                        color = Color.Black.copy(alpha = 0.03f),
                        start = Offset(0f, size.height * 0.35f),
                        end = Offset(size.width, size.height * 0.38f),
                        strokeWidth = 2f
                    )
                }

                drawRect(
                    color = Color(0x3B2C1E14),
                    topLeft = Offset(inset, inset),
                    size = Size(size.width - (inset * 2), size.height - (inset * 2)),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                    )
                )
            }
            .padding(contentPadding)
    ) {
        Column(
            modifier = Modifier.graphicsLayer {
                alpha = (unfoldProgressAnim.value * 3f - 2f).coerceIn(0f, 1f)
            },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(scaledDp(8)),
            content = content
        )
    }
}

@Composable
fun ParchmentHeaderBanner(
    text: String,
    modifier: Modifier = Modifier,
    seed: Long = 777L
) {
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    val bannerScale = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        bannerScale.animateTo(1f, animationSpec = tween(400, easing = FastOutSlowInEasing))
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .graphicsLayer {
                val s = bannerScale.value
                scaleX = s
                scaleY = s
                alpha = s
            }
            .shadow(scaledDp(3), tornShape)
            .background(PapyrusBgLight)
            .border(scaledDp(1), Color(0xFF422112), tornShape)
            .padding(horizontal = scaledDp(24), vertical = scaledDp(6)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFF4E160E),
            fontSize = scaledSp(22),
            fontWeight = FontWeight.ExtraBold,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center
        )
    }
}