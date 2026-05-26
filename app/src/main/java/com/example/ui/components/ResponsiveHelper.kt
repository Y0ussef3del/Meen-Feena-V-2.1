package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Baseline screen width for scaling (e.g., typical phone width)
private const val BASELINE_WIDTH_DP = 360

@Composable
fun rememberScaledDp(dp: Dp): Dp {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = screenWidthDp.toFloat() / BASELINE_WIDTH_DP
    return (dp.value * scale).dp
}

@Composable
fun rememberScaledSp(sp: TextUnit): TextUnit {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val scale = screenWidthDp.toFloat() / BASELINE_WIDTH_DP
    return (sp.value * scale).sp
}

// Convenience inline functions
@Composable
fun scaledDp(value: Int): Dp = rememberScaledDp(value.dp)

@Composable
fun scaledSp(value: Int): TextUnit = rememberScaledSp(value.sp)