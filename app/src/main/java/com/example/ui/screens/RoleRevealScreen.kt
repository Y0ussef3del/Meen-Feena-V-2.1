package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
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
    var revealed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الملفات السرية")
        Spacer(modifier = Modifier.height(10.dp))
        if (!revealed) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(110.dp).background(Color(0x3B6E1C11), CircleShape).padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = "Hide role cards", tint = GoldShine, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "دي التلفون ل : ", color = PapyrusBgLight.copy(alpha = 0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
                Text(text = activePassPlayer.name, color = GoldShine, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("pass_name_reveal"))
                Spacer(modifier = Modifier.height(30.dp))
                Button(onClick = { revealed = true }, colors = ButtonDefaults.buttonColors(containerColor = GoldYell), modifier = Modifier.testTag("reveal_role_button")) {
                    Text("اكتشف الدور السري 👁️", color = Color(0xFF2C150A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            ParchmentCard(modifier = Modifier.weight(1f), seed = state.activePassPlayerIndex.toLong()) {
                Text(text = "الملف السري لـ ${activePassPlayer.name}", color = DarkWoodButton, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier.size(80.dp).background(DarkBg, CircleShape).border(3.dp, GoldYell, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Avatar", tint = GoldShine, modifier = Modifier.size(50.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                val char = activePassPlayer.character
                if (char != null) {
                    Text("الاسم : ${char.name}", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("السن : ${char.age} سنة | المهنة: ${char.occupation}", color = PapyrusTextSecondary, fontSize = 15.sp)
                    Text("الصفات : ${char.traits}", color = PapyrusTextSecondary, fontSize = 15.sp, fontStyle = FontStyle.Italic)
                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color(0x3B2C1E14), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (activePassPlayer.isMafia) RedAccent else InnocentAccent).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security, contentDescription = "Role Symbol", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (activePassPlayer.isMafia) "أنت : المجرم الحقيقية" else "أنت : بريء من الجريمة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "دافعك المستخبي:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = if (activePassPlayer.isMafia) char.hiddenMotive else "انت برئ حاول تكتشف المجرم الحقيقي !!", color = PapyrusText, fontSize = 15.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.confirmSecretsRevealed(); revealed = false },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_reveal_advance"),
                contentPadding = PaddingValues(14.dp)
            ) {
                Text(text = if (state.activePassPlayerIndex < state.players.size - 1) "خبي ملفك وهات اللي بعده" else "يلا ندخل على تفاصيل القضية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}