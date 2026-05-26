package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke  // ✅ إضافة الاستيراد المفقود
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*
import kotlin.random.Random

// Note: scaledDp and scaledSp are imported from ResponsiveHelpers.kt (no redefinition)

fun createTornPaperShape(seed: Long = 42L): GenericShape {
    return GenericShape { size, _ ->
        val rand = Random(seed)
        val numPoints = 60

        moveTo(0f, 0f)
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f)
            lineTo(x, y)
        }
        for (i in 0..numPoints) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = size.width + (if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f))
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val x = size.width * fraction
            val y = size.height + (if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f))
            lineTo(x, y)
        }
        for (i in numPoints downTo 0) {
            val fraction = i.toFloat() / numPoints
            val y = size.height * fraction
            val x = if (i == 0 || i == numPoints) 0f else (rand.nextFloat() * 8f - 4f)
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
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    val responsiveElevation = scaledDp(elevation.value.toInt())

    // Pre-calculate responsive values for drawing (outside drawBehind)
    val inset = scaledDp(12).value
    val strokeWidth = scaledDp(2).value

    Box(
        modifier = modifier
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
                val path = android.graphics.Path()
                val rand = Random(seed + 1)
                path.moveTo(inset, inset)
                path.lineTo(size.width - inset, inset + (rand.nextFloat() * 4f - 2f))
                path.lineTo(size.width - inset + (rand.nextFloat() * 4f - 2f), size.height - inset)
                path.lineTo(inset, size.height - inset + (rand.nextFloat() * 4f - 2f))
                path.lineTo(inset + (rand.nextFloat() * 4f - 2f), inset)
                drawPath(
                    path = path.asComposePath(),
                    color = Color(0x3B2C1E14),
                    style = Stroke(
                        width = strokeWidth,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 5f), 0f)
                    )
                )
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
    val tornShape = remember(seed) { createTornPaperShape(seed) }
    Box(
        modifier = modifier
            .wrapContentSize()
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