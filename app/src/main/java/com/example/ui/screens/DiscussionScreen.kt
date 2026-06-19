package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
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

@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val timeLeft = state.discussionTimeLeft
    val totalTime = state.settings.discussionTimeMinutes * 60
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val isHost = state.hostId == viewModel.myPlayerId.value
    val suspects = state.players.filter { it.isAlive }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "طاولة نقاش المشتبهين")
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(color = Color(0x3B2C1E14), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 6.dp.toPx()))
                    drawArc(color = if (progress > 0.25f) DarkWoodButton else RedAccent, startAngle = -90f, sweepAngle = progress * 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Timer, "Timer clock", tint = if (progress > 0.25f) DarkWoodButton else RedAccent, modifier = Modifier.size(24.dp))
                    val mins = timeLeft / 60
                    val secs = timeLeft % 60
                    Text(text = String.format("%02d:%02d", mins, secs), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                items(suspects.size) { index ->
                    val candidate = suspects[index]
                    val isClickSuspected = state.suspectedPlayerIds.contains(candidate.id)
                    Box(modifier = Modifier.size(width = 110.dp, height = 135.dp).background(Color(0xFFF2E6D0), RoundedCornerShape(12.dp)).border(2.dp, if (isClickSuspected) RedAccent else Color(0x3D2C1E14), RoundedCornerShape(12.dp)).clickable { if (isHost) viewModel.togglePlayerSuspicion(candidate.id) }.padding(8.dp), contentAlignment = Alignment.TopCenter) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Box(modifier = Modifier.size(44.dp).background(if (isClickSuspected) RedAccent else DarkWoodButton, CircleShape), contentAlignment = Alignment.Center) {
                                Text(text = candidate.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.background(Color(0x3B000000), RoundedCornerShape(4.dp)).padding(horizontal = 3.dp, vertical = 1.dp)) {
                                Text(text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال", color = if (isClickSuspected) Color.Black else Color.White, fontSize = 8.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentCard(modifier = Modifier.wrapContentHeight(), seed = 771L) {
                Text(text = "تناقشوا في القضية .....القاعدة المهمة الجميع متهم خلي بالك", color = PapyrusTextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (isHost) {
                Button(onClick = { viewModel.playButtonClick(); viewModel.forceAdvanceToVoting() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp).testTag("skip_discussion_button")) {
                    Icon(Icons.Default.HowToVote, "Go to voting ballot", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إنهاء النقاش والانتقال للتصويت السري 🗳️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x242C1E14))) {
                    Text(text = "تناقشوا بحرية.. المضيف سينقلكم للتصويت عند انتهاء الوقت أو يدويًا.", color = PapyrusText, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}