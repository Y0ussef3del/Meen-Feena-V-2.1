package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun scaledDp(value: Int): Dp = (LocalConfiguration.current.screenWidthDp * value / 360f).dp

@Composable
fun scaledSp(value: Int): TextUnit = (LocalConfiguration.current.screenWidthDp * value / 360f).sp