package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
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
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    val char = activePassPlayer.character ?: return
    var revealed by remember { mutableStateOf(false) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "كشف الهوية السرية")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 112L) {
                if (!revealed) {
                    Icon(Icons.Default.Lock, contentDescription = "Secret Identity Locked", tint = RedAccent, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "دور اللاعب الحالي:", color = PapyrusTextSecondary, fontSize = 15.sp)
                    Text(text = activePassPlayer.name, color = Color(0xFF4A1008), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "دي الموبايل لـ ${activePassPlayer.name} ومحدش يبص غيره، وبعدين اضغط الزرار تحت عشان تشوف ملفك السري.", color = PapyrusText, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(onClick = { revealed = true }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("reveal_secret_button"), contentPadding = PaddingValues(14.dp)) {
                        Text(text = "أنا جاهز.. اكشف ملفي السري 🔍", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                } else {
                    Box(modifier = Modifier.size(72.dp).background(if (activePassPlayer.isMafia) Color(0x23E63946) else Color(0x1F2A9D8F), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (activePassPlayer.isMafia) Icons.Default.Warning else Icons.Default.Security, contentDescription = "Role icon", tint = if (activePassPlayer.isMafia) RedAccent else InnocentAccent, modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "ملفك السري يا ${activePassPlayer.name}", color = PapyrusTextSecondary, fontSize = 14.sp)
                    Text(text = if (activePassPlayer.isMafia) "أنت الـمُــجرِم 🩸" else "أنت بـريء  🔍", color = if (activePassPlayer.isMafia) RedAccent else InnocentAccent, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("role_text_reveal"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x0C000000)), border = BorderStroke(1.dp, Color(0x2B2C1E14))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "شخصيتك: ${char.name}", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(text = "المهنة: ${char.occupation} | السن: ${char.age}", color = PapyrusText, fontSize = 14.sp)
                            Text(text = "الصفات: ${char.traits}", color = PapyrusTextSecondary, fontSize = 13.sp)
                            Text(text = "علاقتك بالضحية: ${char.relationshipToVictim}", color = PapyrusText, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (activePassPlayer.isMafia) Color(0x1AE63946) else Color(0x1A2A9D8F))) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "الدافع / المهمة:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = if (activePassPlayer.isMafia) char.hiddenMotive else "انت برئ حاول تكتشف المجرم الحقيقي !!", color = PapyrusText, fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { viewModel.confirmSecretsRevealed(); revealed = false }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_reveal_advance"), contentPadding = PaddingValues(14.dp)) {
                        Text(text = if (state.activePassPlayerIndex < state.players.size - 1) "خبي ملفك وهات اللي بعده" else "يلا ندخل على تفاصيل القضية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}