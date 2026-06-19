package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.ui.components.MysteryBackground
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val eliminatedPlayers = state.players.filter { !it.isAlive }
    val remainingSuspects = state.players.filter { it.isAlive }
    val localPlayer = state.players.find { it.id == viewModel.myPlayerId.value }

    if (state.mode == "PASS_AND_PLAY") {
        val juryVoter = eliminatedPlayers.firstOrNull { it.id !in state.juryVotes.keys }
        var isDevicePassed by remember(juryVoter?.id) { mutableStateOf(false) }

        if (juryVoter != null && !isDevicePassed) {
            MysteryBackground {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "مرر الموبايل")
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = "هات الموبايل ووريه لـ / ${juryVoter.name}",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "تصويتك هيكون مهم ومصير الباقيين في إيدك .... متبقاش غبي !!",
                        color = Color.LightGray,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Button(
                        onClick = { isDevicePassed = true },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("ادخل صوّت ", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                Box(
                    modifier = Modifier.size(80.dp).background(Color(0x3B6E1B10), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Gavel, "Gavel judge", tint = RedAccent, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "!!! لا تقلقوا ولكن احذروا !!!",
                    color = Color(0xFF6E1D10),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "بما ان فضل اتنين فبالله عليكم نفوق شوية ونركز عشان نعرف نطلع المجرم",
                    color = PapyrusTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (juryVoter != null) {
                    Text(
                        text = "دور اللاعب : ${juryVoter.name}",
                        color = RedAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.testTag("jury_voter_title")
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(remainingSuspects, key = { it.id }) { suspect ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(1.dp, Color(0x3B2C1E14), RoundedCornerShape(10.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    suspect.character?.let {
                                        Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    }
                                }
                                Button(
                                    onClick = { viewModel.submitJuryVote(suspect.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                ) {
                                    Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "تم جمع كافة استنتاجات اللاعبين بنجاح. سنعلن النتيجة الآن!",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    } else {
        if (localPlayer == null) return
        val isAlive = localPlayer.isAlive
        val hasVoted = state.juryVotes.containsKey(localPlayer.id)

        MysteryBackground {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (isAlive) {
                    ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "هيئة المحلفين بتصوّت دلوقتي...",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "مصيرك وصاحبك الأخير بين إيدين اللاعبين اللي خرجوا! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟",
                        color = Color.LightGray,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Center
                    )
                } else {
                    if (hasVoted) {
                        ParchmentHeaderBanner(text = "تم تسجيل صوتك للمحلفين! ⚖️")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "مستنيين باقي اللاعبين عشان تظهر النتيجة...",
                            color = PapyrusBgLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                        Spacer(modifier = Modifier.height(16.dp))
                        ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                            Text(
                                text = "اضغط إدانة على المجرم الحقيقي عشان تحسم الجريمة وترجع حق الضحية!",
                                color = Color(0xFF6E1D10),
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth().weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(remainingSuspects, key = { it.id }) { suspect ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                            .border(1.dp, Color(0x3B2C1E14), RoundedCornerShape(10.dp))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                            suspect.character?.let {
                                                Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                            }
                                        }
                                        Button(
                                            onClick = { viewModel.submitJuryVote(suspect.id) },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                        ) {
                                            Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}