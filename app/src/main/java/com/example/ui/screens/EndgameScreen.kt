package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف أوراق القضية النهائية")
        Spacer(modifier = Modifier.height(14.dp))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 4441L) {
            Box(modifier = Modifier.size(100.dp).background(Color(0x1FA2A012), CircleShape).border(2.dp, GoldYell, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy logo endgame", tint = GoldYell, modifier = Modifier.size(64.dp))
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = if (isInnocentsWinner) "!!الف مبرووك عرفتوا تطلعوا المجرم الفاشل!!" else "!المجرم كسب وضحك على الكل!", color = if (isInnocentsWinner) GreenAccent else RedAccent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("endgame_victory_title"))
            Spacer(modifier = Modifier.height(10.dp))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0C000000), RoundedCornerShape(10.dp)).padding(14.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    item {
                        Text(text = if (isInnocentsWinner) "الأبرياء عرفوا يجمعوا الأدلة ويكشفوا اللعبة الصح، والمجرم وقع في شر أعماله ." else "المجرم عرف يضحك على الكل وثبت تهم باطلة على الأبرياء، وخرج من القضية زي الشعرة من العجين.", color = PapyrusText, fontSize = 16.sp, lineHeight = 24.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "الهوية الحقيقية للمجرم:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("dramatic_criminal_reveal_header"))
                        state.players.filter { it.isMafia }.forEach { mafia ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)), border = BorderStroke(1.dp, Color(0xFFE63946)), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(text = "المجرم الحقيقي: ${mafia.name}", color = Color(0xFF3B6E1B10), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.testTag("criminal_character_name"))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("العمر: ${mafia.character?.age ?: 30} سنة | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = 15.sp)
                                    Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("المستوى الاجتماعي: ${mafia.character?.socialStatus ?: "متوسط الحال"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("علاقته بالمشتبهين: ${mafia.character?.relationshipToOtherSuspects ?: "منافسة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Text("السجل الجنائي: ${mafia.character?.relevantHistory ?: "خالي من السوابق"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = "الدافع والنية المستخبية: ${mafia.character?.hiddenMotive ?: ""}", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 15.sp, lineHeight = 20.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(text = "المخطط الكامل وسيناريو الجريمة الداخلي:", color = Color(0xFF355E3B), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().testTag("case_explanation_header"))
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1F2A9D8F)), shape = RoundedCornerShape(8.dp)) {
                            Text(text = currentCase?.explanation ?: "لم تتوفر سجلات سردية للملف.", color = Color(0xFF1D3557), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(12.dp).testTag("case_explanation_text"))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = "كشف هويات كل اللاعبين بغرفة التحقيق:", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.players.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (p.isMafia) "مجرم" else "بريء ", color = if (p.isMafia) RedAccent else InnocentAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${p.name} (${p.character?.name ?: ""})", color = PapyrusTextSecondary, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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