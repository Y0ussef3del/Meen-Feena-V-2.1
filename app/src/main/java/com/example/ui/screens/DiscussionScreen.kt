package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val suspectedByClick = remember { mutableStateListOf<String>() 
    val context = LocalContext.current
    LaunchedEffect(Unit) {
    com.example.game.audio.MysteryAudioPlayer.lowerVolumeForDiscussion()
}
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        val minSide = minOf(maxWidth, maxHeight)
        val timerSize = (minSide * 0.34f).coerceIn(120.dp, 170.dp)
        val avatarSize = (minSide * 0.14f).coerceIn(52.dp, 68.dp)
        val radius = (minSide * 0.32f).coerceIn(90.dp, 150.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "مرحلة النقاش والمواجهة")

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val formattedTime = String.format("%02d:%02d", state.timerSecondsLeft / 60, state.timerSecondsLeft % 60)

                Canvas(modifier = Modifier.size(timerSize)) {
                    drawCircle(color = Color(0xFF1E0604), radius = size.minDimension / 2)
                    val sweepAngle = if (state.timerTotalSeconds > 0) {
                        (state.timerSecondsLeft.toFloat() / state.timerTotalSeconds.toFloat()) * 360f
                    } else 360f

                    drawArc(
                        color = Color(0xFFE73224),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("متبقي", color = GoldYell, fontSize = 12.sp)
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("timer_countdown_display")
                    )
                    Text("للإدلاء بالاستنتاج", color = PapyrusBgLight.copy(alpha = 0.5f), fontSize = 10.sp)
                }

                val alivePlayers = state.players.filter { it.isAlive }

                alivePlayers.forEachIndexed { index, player ->
                    val angleRad = (2 * Math.PI * index) / maxOf(alivePlayers.size, 1)
                    val xOffset = (radius.value * cos(angleRad)).dp
                    val yOffset = (radius.value * sin(angleRad)).dp
                    val isClickSuspected = player.id in suspectedByClick

                    Box(
                        modifier = Modifier
                            .offset(x = xOffset, y = yOffset)
                            .size(avatarSize)
                            .shadow(3.dp, CircleShape)
                            .background(
                                if (isClickSuspected) Color(0xFFC42512) else Color(0xFF421E14),
                                CircleShape
                            )
                            .border(
                                2.dp,
                                if (isClickSuspected) GoldShine else Color(0x3BFFFFFF),
                                CircleShape
                            )
                            .clickable {
                                if (isClickSuspected) suspectedByClick.remove(player.id)
                                else suspectedByClick.add(player.id)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = player.name.take(6),
                                color = Color.White,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .background(
                                        Color(0x3B000000),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 3.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال",
                                    color = if (isClickSuspected) Color.Black else Color.White,
                                    fontSize = 7.sp
                                )
                            }
                        }
                    }
                }
            }

            ParchmentCard(modifier = Modifier.wrapContentHeight(), seed = 771L) {
                Text(
                    text = "تناقشوا في القضية .....القاعدة المهمة الجميع متهم خلي بالك",
                    color = PapyrusTextSecondary,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

        Button(
    onClick = { 
        MysteryAudioPlayer.playClick() 
        viewModel.advanceFromDiscussionToVoting() 
    },
    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
    shape = RoundedCornerShape(12.dp),
    modifier = Modifier
        .fillMaxWidth()
        .navigationBarsPadding()
        .heightIn(min = 56.dp)
        .testTag("voting_advance_button")
) {
    Icon(Icons.Default.HowToVote, "Start Votes", tint = GoldShine)
    Spacer(modifier = Modifier.width(8.dp))
    Text("يلا ندخل على التصويت", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 20.sp)
}
        }
    }
}