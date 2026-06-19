package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "تفاصيل الجريمة")
        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
            Text(text = currentCase.title, color = Color(0xFF7A1B0C), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
                Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                    Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x12000000), RoundedCornerShape(8.dp)).padding(12.dp)) {
                LazyColumn { item { Text(text = currentCase.description, color = PapyrusText, fontSize = 14.sp, lineHeight = 21.sp, textAlign = TextAlign.End) } }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = " المشتبه فيهم : ", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                currentCase.characters.forEach { char ->
                    Box(modifier = Modifier.weight(1f).background(Color(0xFF8C2012), RoundedCornerShape(6.dp)).padding(6.dp), contentAlignment = Alignment.Center) {
                        Text(text = char.name.split(" ").firstOrNull() ?: char.name, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.startCaseInvestigationIntro() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), modifier = Modifier.fillMaxWidth().testTag("case_details_confirm_button"), contentPadding = PaddingValues(15.dp)) {
            Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}