package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "تفاصيل الجريمة")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
                Text(text = currentCase.title, color = Color(0xFF7A1B0C), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                    Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                        Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "ملخص القضية والتقرير الجنائي:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = currentCase.description, color = PapyrusText, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = { viewModel.confirmSecretsRevealed() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(54.dp).testTag("start_investigation_action_button")) {
                Icon(Icons.Default.Search, contentDescription = "Investigate clues", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}