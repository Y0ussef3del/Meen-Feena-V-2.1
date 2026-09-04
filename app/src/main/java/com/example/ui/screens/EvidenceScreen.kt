package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    val clueIndex = state.currentEvidenceIndex
    val currentClue = currentCase.evidenceList.getOrElse(clueIndex) { "لا أدلة إضافية حالياً." }
    var showHint by remember(clueIndex) { mutableStateOf(false) }

    val isHost = state.mode != "ONLINE" || state.hostId == viewModel.myPlayerId.value

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "الدليل  ${clueIndex + 1} من ${currentCase.evidenceList.size}")
        Spacer(modifier = Modifier.height(12.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = (clueIndex + 10).toLong()) {
            Box(modifier = Modifier.size(90.dp).background(Color(0xFF35120D), CircleShape).border(2.dp, GoldShine, CircleShape).shadow(4.dp, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, contentDescription = "Evidence Seal", tint = GoldShine, modifier = Modifier.size(48.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "ركز في الدليل بالله عليك عشان متضيعناش", color = Color(0xFF531E17), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0F000000), RoundedCornerShape(10.dp)).padding(14.dp), contentAlignment = Alignment.Center) {
                Text(text = currentClue, color = PapyrusText, fontSize = 15.sp, lineHeight = 22.sp, textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(10.dp))
            AnimatedVisibility(visible = showHint) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF2CD)).border(1.dp, Color(0xFFFFCD56), RoundedCornerShape(8.dp)).padding(10.dp)) {
                    Text(text = currentCase.hint, color = Color(0xFF856404), fontSize = 12.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            if (!showHint) {
                Button(onClick = { showHint = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A012)), modifier = Modifier.testTag("clue_hint_button")) {
                    Icon(Icons.Default.Warning, "Clues Alert", tint = Color.Black, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض تلميح  💡", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isHost) {
            Button(
                onClick = { viewModel.advanceFromEvidenceToDiscussion() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.fillMaxWidth().testTag("evidence_reveal_advance"),
                contentPadding = PaddingValues(15.dp)
            ) {
                Icon(Icons.Default.RecordVoiceOver, "Discuss", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("خش علي المناقشة🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        } else {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1F2C1E14)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "في انتظار المضيف لبدء المناقشة...",
                    color = GoldShine,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(14.dp)
                )
            }
        }
    }
}