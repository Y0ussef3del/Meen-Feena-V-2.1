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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.game.audio.MysteryAudioPlayer
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*

@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val isPassAndPlay = state.mode == "PASS_AND_PLAY"
    val activePassPlayer = if (isPassAndPlay) {
        state.players.getOrNull(state.activePassPlayerIndex)
    } else {
        state.players.find { it.id == viewModel.myPlayerId.value }
    } ?: return

    val rememberKey = if (isPassAndPlay) state.activePassPlayerIndex.toString() else activePassPlayer.id
    var revealed by remember(rememberKey) { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الشخصيات")
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
                Text(text = "ادي التلفون ل : ", color = PapyrusBgLight.copy(alpha = 0.8f), fontSize = 16.sp, textAlign = TextAlign.Center)
                Text(text = activePassPlayer.name, color = GoldShine, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("pass_name_reveal"))
                Spacer(modifier = Modifier.height(30.dp))
                Button(
                    onClick = {
                        MysteryAudioPlayer.playClick(context)
                        revealed = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldYell),
                    modifier = Modifier.testTag("reveal_role_button")
                ) {
                    Text("اكتشف الدور السري 👁️", color = Color(0xFF2C150A), fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            ParchmentCard(modifier = Modifier.weight(1f), seed = if (isPassAndPlay) state.activePassPlayerIndex.toLong() else 42L) {
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
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0x3B2C1E14), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(if (activePassPlayer.isMafia) RedAccent else InnocentAccent).padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security, contentDescription = "Role Symbol", tint = Color.White, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = if (activePassPlayer.isMafia) "أنت : المجرم الحقيقي" else "أنت : بريء من الجريمة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("الدافع : ${char.hiddenMotive}", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Button(
                onClick = {
                    MysteryAudioPlayer.playClick(context)
                    viewModel.confirmSecretsRevealed()
                    revealed = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = if (isPassAndPlay && state.activePassPlayerIndex < state.players.size - 1)
                        "خبي ملفك وهات اللي بعده"
                    else
                        "يلا على تفاصيل القضية",
                    color = GoldShine,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 20.sp
                )
            }
        }
    }
}