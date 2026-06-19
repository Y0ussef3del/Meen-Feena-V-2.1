package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.theme.*

@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismissRequest: () -> Unit) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember { mutableStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember { mutableStateOf(state.settings.votingTimeMinutes) }
    var soundEnabled by remember { mutableStateOf(state.settings.isMusicEnabled) }
    var sliderVol by remember { mutableStateOf(state.settings.volume) }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = { viewModel.updateSettings(discTimeMins, voteTimeMins, soundEnabled, sliderVol); onDismissRequest() },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التعديلات", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequest) { Text("إلغاء", color = PapyrusTextSecondary, fontSize = 16.sp) } },
        title = { Text(text = "إعدادات وقواعد اللعبة", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 22.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            ParchmentCard(seed = 77L, contentPadding = PaddingValues(12.dp), modifier = Modifier.wrapContentHeight()) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = RedAccent))
                            Text("المؤثرات الصوتية والموسيقى", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("درجة الصوت: ${(sliderVol * 100).toInt()}%", color = PapyrusTextSecondary, fontSize = 14.sp)
                        Slider(value = sliderVol, onValueChange = { sliderVol = it }, modifier = Modifier.fillMaxWidth())
                        Divider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("وقت جولات المناقشة", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { if (discTimeMins > 1) discTimeMins-- }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text("$discTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(onClick = { if (discTimeMins < 10) discTimeMins++ }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("وقت جولات التصويت", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { if (voteTimeMins > 1) voteTimeMins-- }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("-", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                            Text("$voteTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.align(Alignment.CenterVertically))
                            Button(onClick = { if (voteTimeMins < 5) voteTimeMins++ }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("+", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Divider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("قوانين اللعبة الأساسية:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(text = "1. اللعبة تدعم من 4 لـ 6 لاعبين.\n2. لو عدد اللاعبين 4، بيكون فيه مجرم واحدة بس؛ ولو أكتر من كدة بيتم تعيين 2 مجرم تلقائياً لدعم التحدي والمنافسة.\n3. في نهاية الجولة لو اتبقى اتنين مشتبه بيهم بس عايشين، بيتلغي تصويت الاقتراع المباشر واللعيبة اللي خرجوا بترجع تلقائياً كـ (هيئة المحلفين) لحسم القرار النهائي وإدانة المجرم الحقيقية.", color = PapyrusTextSecondary, fontSize = 15.sp, lineHeight = 22.sp)
                    }
                }
            }
        },
        containerColor = PapyrusBg
    )
}