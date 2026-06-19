package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun ThrillerTitleComponent(
    fontSize: TextUnit = 65.sp,
    maxWidth: Dp? = null
) {
    val adjustedSize = if (maxWidth != null) responsiveTitleSize(maxWidth) else fontSize
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "مين فينا ؟",
            color = GoldYell,
            fontSize = adjustedSize,
            fontWeight = FontWeight.Black,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_logo_arabic")
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun SplashScreen() {
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val currentMaxWidth = maxWidth
        val padding = responsivePadding(currentMaxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ThrillerTitleComponent(fontSize = responsiveTitleSize(currentMaxWidth) * 0.83f, maxWidth = currentMaxWidth)
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                color = RedAccent,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                text = "الكل متهم .......ولكن ؟",
                color = PapyrusBgLight.copy(alpha = 0.5f),
                fontSize = if (currentMaxWidth < 360.dp) 18.sp else 30.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}