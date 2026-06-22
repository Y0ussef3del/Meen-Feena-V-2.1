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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.random.Random
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Immutable

// توحيد الـ Animation Spec كـ Private constant (تعديل 17)
private val PaperUnfoldAnimationSpec = tween<Float>(durationMillis = 750, easing = FastOutSlowInEasing)

// هيكل بيانات الـ Seed الثابتة لمنع Recomposition غير الضرورية (تعديل 18)
@Immutable
data class ParchmentPointsData(
    val topOffsets: List<Float>,
    val rightOffsets: List<Float>,
    val bottomOffsets: List<Float>,
    val leftOffsets: List<Float>
)

// توليد نقاط التمزق كنسبة مئوية مسبقاً (مرفوعة من كود الرسم - تعديل 4)
fun generateTornPoints(seed: Long, numPoints: Int): ParchmentPointsData {
    val rand = Random(seed)
    val generate: () -> List<Float> = {
        List(numPoints + 1) { index ->
            if (index == 0 || index == numPoints) 0f else (rand.nextFloat() * 2f - 1f) // تتراوح بين -1 و 1
        }
    }
    return ParchmentPointsData(generate(), generate(), generate(), generate())
}

fun createTornPaperShape(points: ParchmentPointsData, numPoints: Int): GenericShape {
    return GenericShape { size, _ ->
        // جعل عمق التمزق Responsive بناءً على أبعاد الكارت (تعديل 11)
        val maxDev = size.minDimension * 0.012f

        moveTo(0f, 0f)
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = if (i == 0 || i == numPoints) 0f else (points.topOffsets[i] * maxDev)
            lineTo(x, y)
        }
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = size.width + (if (i == 0 || i == numPoints) 0f else (points.rightOffsets[i] * maxDev))
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = size.height + (if (i == 0 || i == numPoints) 0f else (points.bottomOffsets[i] * maxDev))
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = if (i == 0 || i == numPoints) 0f else (points.leftOffsets[i] * maxDev)
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
    content: @Composable ColumnScope.() -> Unit
) {
    // تقليل النقاط إلى 24 لرفع الأداء (تعديل 10)
    val numPoints = 24
    val pointsData = remember(seed) { generateTornPoints(seed, numPoints) }
    val tornShape = remember(pointsData) { createTornPaperShape(pointsData, numPoints) }
    
    val responsiveElevation = scaledDp(elevation.value.toInt())
    val inset = scaledDp(12).value
    val strokeWidth = scaledDp(2).value

    // استخدام animateFloatAsState لتقليل عدد الـ Coroutines (تعديل 6)
    var isMounted by remember { mutableStateOf(false) }
    LaunchedEffect(seed) { isMounted = true } // تعديل 9 (ربط بـ seed لإعادة الحساب)

    val unfoldProgress by animateFloatAsState(
        targetValue = if (isMounted) 1f else 0f,
        animationSpec = PaperUnfoldAnimationSpec,
        label = "PaperUnfold"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                rotationX = (1f - unfoldProgress) * -20f
                scaleX = 0.88f + (unfoldProgress * 0.12f)
                scaleY = 0.88f + (unfoldProgress * 0.12f)
                alpha = unfoldProgress
                cameraDistance = 1200f // تعديل المنظور لتجنب التشوه البصري (تعديل 8)
            }
            .shadow(
                elevation = responsiveElevation * unfoldProgress,
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
            // استبدال drawBehind بـ drawWithCache لمنع بناء الكائنات بكل Frame (تعديل 5 و 7)
            .drawWithCache {
                val internalPath = android.graphics.Path()
                val maxDev = size.minDimension * 0.006f
                val rand = Random(seed + 1)

                internalPath.moveTo(inset, inset)
                internalPath.lineTo(size.width - inset, inset + (rand.nextFloat() * maxDev * 2 - maxDev))
                internalPath.lineTo(size.width - inset + (rand.nextFloat() * maxDev * 2 - maxDev), size.height - inset)
                internalPath.lineTo(inset, size.height - inset + (rand.nextFloat() * maxDev * 2 - maxDev))
                internalPath.lineTo(inset + (rand.nextFloat() * maxDev * 2 - maxDev), inset)
                
                val composePath = internalPath.asComposePath()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)

                onDrawBehind {
                    drawPath(
                        path = composePath,
                        color = Color(0x3B2C1E14),
                        style = Stroke(width = strokeWidth, pathEffect = dashEffect)
                    )
                }
            }
            .padding(contentPadding)
    ) {
        Column(
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
    val numPoints = 24
    val pointsData = remember(seed) { generateTornPoints(seed, numPoints) }
    val tornShape = remember(pointsData) { createTornPaperShape(pointsData, numPoints) }
    
    var isMounted by remember { mutableStateOf(false) }
    LaunchedEffect(seed) { isMounted = true }

    val bannerUnfold by animateFloatAsState(
        targetValue = if (isMounted) 1f else 0f,
        animationSpec = PaperUnfoldAnimationSpec,
        label = "BannerUnfold"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = bannerUnfold
                alpha = bannerUnfold
            }
            .wrapContentSize()
            .shadow(scaledDp(3), tornShape)
            .clip(tornShape) // تم تقديم الـ clip هنا ليتم قطع الخلفية والحدود بشكل ممزق (تعديل 12)
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