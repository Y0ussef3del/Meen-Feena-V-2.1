package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val mafia = state.players.find { it.isMafia }
    val isMafiaWin = state.players.filter { !it.isMafia }.all { !it.isAlive }
    val winnerText = if (isMafiaWin) "🩸 انـتـصـر الـمُـجْـرِم والعدالة فشلت!" else "🔍 عـاش الـمُـحَـقِّـقُـون! تم كشف المجرم"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "نهاية التحقيق الجنائي")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 1209L) {
                Box(modifier = Modifier.size(76.dp).background(Color(0x1F2C1E14), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, "Victory cup trophy", tint = GoldYell, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = winnerText, color = Color(0xFF4A1008), fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.testTag("endgame_victory_text"))
                Spacer(modifier = Modifier.height(10.dp))
                if (mafia != null) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)), border = BorderStroke(1.dp, Color(0xFFE63946)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "المجرم الحقيقي: ${mafia.name}", color = Color(0xFF3B6E1B10), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.testTag("criminal_character_name"))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("العمر: ${mafia.character?.age ?: 30} سنة | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = 15.sp)
                            Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x0C000000))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "جدول أدوار اللاعبين الصادقة:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.players.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (p.isMafia) "مجرم" else "بريء ", color = if (p.isMafia) RedAccent else InnocentAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${p.name} (${p.character?.name ?: ""})", color = PapyrusTextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(padding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { viewModel.playButtonClick(); viewModel.playAgain() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("play_again_button"), contentPadding = PaddingValues(15.dp)) {
                    Icon(Icons.Default.Refresh, "Play again", tint = GoldShine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("لعب جولة وقضية جديدة", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                OutlinedButton(onClick = { viewModel.playButtonClick(); viewModel.resetToMainMenu() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                    Icon(Icons.Default.Home, "Main menu", tint = GoldShine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("العودة للقائمة الرئيسية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
    }
}