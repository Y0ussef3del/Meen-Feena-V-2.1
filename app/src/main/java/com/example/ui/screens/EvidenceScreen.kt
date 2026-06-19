package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val clueText = currentCase.evidenceList.getOrNull(clueIndex) ?: return

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "الدليل الجنائي ${clueIndex + 1} من ${currentCase.evidenceList.size}")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = (clueIndex + 10).toLong()) {
                Box(modifier = Modifier.size(90.dp).background(Color(0xFF35120D), CircleShape).border(2.dp, GoldShine, CircleShape).shadow(4.dp, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Search, contentDescription = "Evidence Seal", tint = GoldShine, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "تقرير الدليل والشهادة", color = RedAccent, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = clueText, color = PapyrusText, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { viewModel.confirmSecretsRevealed() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp).testTag("next_evidence_action_button")) {
                Text(text = if (clueIndex < currentCase.evidenceList.size - 1) "الدليل التالي ➡️" else "اقفل المحضر وادخل للنقاش 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}