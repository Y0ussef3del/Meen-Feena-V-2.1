package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.game.audio.MysteryAudioPlayer
import com.example.ui.components.MysteryBackground
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current

    if (state.mode == "PASS_AND_PLAY") {
        val voterPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
        var isDevicePassed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }

        if (!isDevicePassed) {
            MysteryBackground {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "صندوق التصويت")
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(text = "ادي الموبايل لـ/ ${voterPlayer.name}", color = PapyrusBgLight, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "فكر قبل ما تصوت ...شغل دماغك !!!", color = Color.LightGray, fontSize = 17.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = {
                            MysteryAudioPlayer.playClick(context)
                            isDevicePassed = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("يلا نصوّت", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        var selectedTargetId by remember(state.activePassPlayerIndex) { mutableStateOf("") }
        val eligibleCandidates = remember(state.tiedVotePlayers, state.players, voterPlayer.id) {
            if (state.tiedVotePlayers.isNotEmpty()) {
                state.players.filter { it.id in state.tiedVotePlayers && it.id != voterPlayer.id }
            } else {
                state.players.filter { it.isAlive && it.id != voterPlayer.id }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "وقت التصويت")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 33L) {
                Text(text = "دور اللاعب: ${voterPlayer.name}", color = Color(0xFF6E1B10), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "صوت علي اللي شاكك فيه", color = PapyrusTextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(eligibleCandidates, key = { it.id }) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp))
                                .border(2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp))
                                .clickable {
                                    MysteryAudioPlayer.playClick(context)
                                    selectedTargetId = candidate.id
                                }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(38.dp).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                candidate.character?.let { Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp) }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    if (selectedTargetId.isBlank()) {
                        MysteryAudioPlayer.playClick(context)
                        Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                    } else {
                        MysteryAudioPlayer.playClick(context)
                        viewModel.submitVote(selectedTargetId)
                        selectedTargetId = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_vote_action_button")
            ) {
                Text("اللي بعده", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    } else {
        val localVoter = state.players.find { it.id == viewModel.myPlayerId.value } ?: return
        val activePlayers = remember(state.players) { state.players.filter { it.isAlive } }
        val waitingPlayers = remember(activePlayers, state.votes) { activePlayers.filter { it.id !in state.votes.keys } }
        val votesCast = remember(state.votes, state.players) {
            state.votes.mapNotNull { (vId, tId) ->
                val voterName = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
                val targetName = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
                "👈 اللاعب $voterName صوّت ضد $targetName"
            }
        }

        if (!localVoter.isAlive || state.votes.containsKey(localVoter.id)) {
            val titleText = if (!localVoter.isAlive) " أنت برة اللعب دلوقتي 💀" else "تم تسجيل صوتك بنجاح! 🗳️"
            val subText = if (!localVoter.isAlive) "الف مبرووك اقعد جمب اخواتك" else "مستنيين باقي اللعيبة يصوتوا..."

            MysteryBackground {
                Column(modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ParchmentHeaderBanner(text = titleText)
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(text = subText, color = PapyrusBgLight, fontSize = if (!localVoter.isAlive) 24.sp else 25.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(20.dp))
                    ParchmentCard(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("الشفافية والتصويت المفتوح المباشر:", color = Color(0xFF6E1B10), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            if (votesCast.isEmpty()) {
                                Text("في انتظار الصوت العلني الأول لبدء كشف التواطؤ... 🗳️", color = PapyrusTextSecondary, fontSize = 14.sp)
                            } else {
                                votesCast.forEach { voteLine ->
                                    Text(text = voteLine, color = PapyrusText, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("مين اللي لسه مصوّتش:", color = Color(0xFF6E1B10), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            val waitingNames = remember(waitingPlayers) { waitingPlayers.joinToString { it.name }.ifEmpty { "الجميع أدلى بصوته علناً!" } }
                            Text(waitingNames, color = PapyrusTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
            }
        } else {
            var selectedTargetId by remember { mutableStateOf("") }
            val eligibleCandidates = remember(state.tiedVotePlayers, state.players, localVoter.id) {
                if (state.tiedVotePlayers.isNotEmpty()) {
                    state.players.filter { it.id in state.tiedVotePlayers && it.id != localVoter.id }
                } else {
                    state.players.filter { it.isAlive && it.id != localVoter.id }
                }
            }

            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "وقت التصويت")
                Spacer(modifier = Modifier.height(10.dp))
                ParchmentCard(modifier = Modifier.weight(1f), seed = 33L) {
                    Text(text = "دورك في التصويت: ${localVoter.name}", color = Color(0xFF6E1B10), fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(text = "اختار الشخص اللي شاكك فيه ان هو المجرم:", color = PapyrusTextSecondary, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(eligibleCandidates, key = { it.id }) { candidate ->
                            val isSelected = candidate.id == selectedTargetId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp))
                                    .clickable {
                                        MysteryAudioPlayer.playClick(context)
                                        selectedTargetId = candidate.id
                                    }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(38.dp).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    candidate.character?.let { Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp) }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (selectedTargetId.isBlank()) {
                            MysteryAudioPlayer.playClick(context)
                            Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                        } else {
                            MysteryAudioPlayer.playClick(context)
                            viewModel.submitVote(selectedTargetId)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_vote_action_button")
                ) {
                    Text("اللي بعده", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}