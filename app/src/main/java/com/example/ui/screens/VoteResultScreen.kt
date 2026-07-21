package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.model.GamePhase
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.MysteryBackground
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.GoldShine
import com.example.ui.theme.PapyrusBgLight
import com.example.ui.theme.RedAccent

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = remember(state.mode, state.hostId, viewModel.myPlayerId.value) {
        state.mode == "PASS_AND_PLAY" || state.hostId == viewModel.myPlayerId.value
    }
    val context = LocalContext.current

    LaunchedEffect(state.lastEliminatedResult, state.phase) {
        if (state.phase == GamePhase.VOTE_RESULT) {
            if (state.lastEliminatedResult.contains("مجرم")) {
                MysteryAudioPlayer.playPlayerEliminatedCriminal(context)
            } else if (state.lastEliminatedResult.isNotBlank() && !state.lastEliminatedResult.contains("تعادل")) {
                MysteryAudioPlayer.playPlayerEliminatedInnocent(context)
            }
        }
    }

    val votesSummary = remember(state.votes, state.players) {
        state.votes.mapNotNull { (vId, tId) ->
            val voter = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
            val target = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
            "👤 $voter 👈 صوّت ضد  $target"
        }
    }

    MysteryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ParchmentHeaderBanner(text = "نتيجة التصويت")
            Spacer(modifier = Modifier.height(24.dp))
            ParchmentCard(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = if (state.tiedVotePlayers.isNotEmpty()) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = "Result Icon",
                        tint = if (state.tiedVotePlayers.isNotEmpty()) Color(0xFFC62828) else GoldShine,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.lastEliminatedResult,
                        color = Color(0xFF1C130C),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp,
                        modifier = Modifier.testTag("vote_result_text")
                    )
                    if (state.tiedVotePlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "قانون تصفية التعادل: سيتم تكرار جولة التصويت الآن لتكون محصورة ومقتصرة فقط على المشتبهين المتساوين بالأصوات حتى التوصل إلى أغلبية حاسمة تفصل الشك بالحقيقة.",
                            color = Color(0xFFB71C1C),
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = "كشف الأصوات العامة  : 🗳️", color = Color(0xFF6E1B10), fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))

                    if (votesSummary.isEmpty()) {
                        Text("لم يتم الإدلاء بأي أصوات.", color = Color.Gray, fontSize = 14.sp)
                    } else {
                        votesSummary.forEach { voteText ->
                            Text(
                                text = voteText,
                                color = Color(0xFF2C1E14),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (isHost) {
                Button(
                    onClick = { viewModel.playButtonClick(); viewModel.confirmVoteResultAndProceed() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_vote_result_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (state.tiedVotePlayers.isNotEmpty()) "بدء جولة حسم التعادل" else "متابعة مسار التحقيق",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "في انتظار المضيف لمتابعة القضية...",
                        color = PapyrusBgLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    )
                }
            }
        }
    }
}