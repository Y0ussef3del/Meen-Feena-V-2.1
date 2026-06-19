package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.hostId == viewModel.myPlayerId.value
    val votesSummary = state.votesResultSummary

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "نتائج الفرز والجرائم")
            Spacer(modifier = Modifier.height(16.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 882L) {
                Icon(Icons.Default.Analytics, "Stats results", tint = DarkWoodButton, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "محضر الفرز القضائي للأصوات:", color = Color(0xFF4A1008), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0C000000), RoundedCornerShape(10.dp)).border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(10.dp)).padding(12.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        if (votesSummary.isEmpty()) {
                            item { Text("لم يتم الإدلاء بأي أصوات.", color = Color.Gray, fontSize = 14.sp) }
                        } else {
                            items(votesSummary.size) { index ->
                                Text(text = votesSummary[index], color = Color(0xFF2C1E14), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                if (state.tiedVotePlayers.isNotEmpty()) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0x26E63946)), border = BorderStroke(1.dp, RedAccent), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(text = "⚠️ تعادل أصوات بين بعض المشتبهين! الجولة تحتاج لحسم.", color = Color(0xFF4A1008), fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(10.dp))
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (isHost) {
                Button(onClick = { viewModel.playButtonClick(); viewModel.confirmVoteResultAndProceed() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_vote_result_button"), shape = RoundedCornerShape(12.dp)) {
                    Text(text = if (state.tiedVotePlayers.isNotEmpty()) "بدء جولة حسم التعادل" else "متابعة مسار التحقيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)), shape = RoundedCornerShape(12.dp)) {
                    Text(text = "في انتظار المضيف لمتابعة القضية...", color = PapyrusBgLight, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}