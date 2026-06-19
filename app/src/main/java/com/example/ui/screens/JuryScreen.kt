package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val myPlayer = state.players.find { it.id == viewModel.myPlayerId.value }
    val isAlive = myPlayer?.isAlive ?: true
    val tiedPlayers = state.players.filter { state.tiedVotePlayers.contains(it.id) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val padding = responsivePadding(this.maxWidth)
        if (isAlive) {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "هيئة المحلفين بتصوّت دلوقتي...", color = PapyrusBgLight, fontSize = 24.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "مصيرك وصاحبك الأخير بين إيدين اللاعبين اللي خرجوا! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟", color = Color.LightGray, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                Spacer(modifier = Modifier.height(10.dp))
                ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                    Box(modifier = Modifier.size(80.dp).background(Color(0x3B6E1B10), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Gavel, "Gavel judge", tint = RedAccent, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "!!! لا تقلقوا ولكن احذروا !!!", color = Color(0xFF6E1D10), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(6.dp))
                    if (tiedPlayers.isNotEmpty()) {
                        Text(text = "بما إنك ميت دلوقتي برة القضية، صوتك هو العدل! اختار مين من الاتنين دول تدينه بصفة نهائية لإنهاء التعادل:", color = PapyrusText, fontSize = 14.sp, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(tiedPlayers.size) { index ->
                                val suspect = tiedPlayers[index]
                                Row(modifier = Modifier.fillMaxWidth().background(Color(0x0C000000), RoundedCornerShape(10.dp)).border(1.dp, Color(0x3D2C1E14), RoundedCornerShape(10.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        suspect.character?.let { Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    }
                                    Button(onClick = { viewModel.submitJuryVote(suspect.id) }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent)) {
                                        Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(text = "تم جمع كافة استنتاجات اللاعبين بنجاح. سنعلن النتيجة الآن!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}