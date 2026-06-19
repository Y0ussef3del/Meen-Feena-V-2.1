package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
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
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val candidates = state.players.filter { it.isAlive }
    var selectedTargetId by remember { mutableStateOf("") }
    val isPassPlay = state.mode == "PASS_PLAY"
    val activeVoter = if (isPassPlay) state.players.getOrNull(state.activePassPlayerIndex) else state.players.find { it.id == viewModel.myPlayerId.value }
    val modeText = if (isPassPlay) "دور اللاعب للتصويت السري:" else "صندوق الاقتراع الرقمي"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
            Spacer(modifier = Modifier.height(12.dp))
            if (activeVoter != null) {
                Text(text = modeText, color = PapyrusTextSecondary, fontSize = 14.sp)
                Text(text = activeVoter.name, color = RedAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "اختر الشخص الذي تظن أنه المجرم الحقيقي بناءً على الأدلة والملفات الجنائية. تصويتك سري بالكامل ولن يراه أحد!", color = PapyrusText, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(candidates.size) { index ->
                    val candidate = candidates[index]
                    val isSelected = candidate.id == selectedTargetId
                    Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp)).border(2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp)).clickable { selectedTargetId = candidate.id }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(38.dp).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { if (selectedTargetId.isBlank()) Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show() else viewModel.submitVote(selectedTargetId) }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_vote_action_button")) {
                Text("أأكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
        }
    }
}