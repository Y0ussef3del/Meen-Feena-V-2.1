package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.theme.*

@Composable
fun SettingsDialog(
    viewModel: GameViewModel,
    navController: NavController,
    onDismissRequest: () -> Unit
) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember(state.settings.discussionTimeMinutes) { mutableIntStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember(state.settings.votingTimeMinutes) { mutableIntStateOf(state.settings.votingTimeMinutes) }
    var soundEnabled by remember(state.settings.isMusicEnabled) { mutableStateOf(state.settings.isMusicEnabled) }
    var sliderVol by remember(state.settings.volume) { mutableFloatStateOf(state.settings.volume) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            Button(
                onClick = {
                    viewModel.updateSettings(discTimeMins, voteTimeMins, soundEnabled, sliderVol)
                    onDismissRequest()
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("حفظ التعديلات", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("إلغاء", color = PapyrusTextSecondary, fontSize = 16.sp)
            }
        },
        title = {
            Text(
                text = "إعدادات وقواعد اللعبة",
                color = Color(0xFF4A1008),
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            ParchmentCard(seed = 77L, contentPadding = PaddingValues(12.dp), modifier = Modifier.wrapContentHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Switch(
                            checked = soundEnabled,
                            onCheckedChange = { soundEnabled = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = RedAccent)
                        )
                        Text("المؤثرات الصوتية والموسيقى", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("درجة الصوت: ${(sliderVol * 100).toInt()}%", color = PapyrusTextSecondary, fontSize = 14.sp)
                    Slider(
                        value = sliderVol,
                        onValueChange = { sliderVol = it },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                    Button(
                        onClick = {
                            onDismissRequest()
                            navController.navigate("cases_library")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
                    ) {
                        Text("⚙️ طور صناعة واستيراد القضايا", color = GoldShine, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0x3B2C1E14))
                    Spacer(modifier = Modifier.height(12.dp))

                    // بطاقة مميزة للمطور الرئيسي
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, GoldYell, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0x2DA2A012)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = " برمجة وفكرة وتصميم",
                                color = Color(0xFF4A1008),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Youssef Adel",
                                color = Color(0xFF2C1E14),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // عنوان المساهمين
                    Text(
                        text = "شكر خاص لكل من ساهموا في خروج اللعبة للنور :",
                        color = Color(0xFF4A1008),
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // عرض الأسماء في شبكة متناسقة (2 في كل صف)
                    val contributors = listOf(
                        "Mohamed Gamal", "Omar Abdelslam",
                        "Mohamed Ashraf", "Eslam Gbr",
                        "Ahmed Rabea", "Adham Magdy"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        contributors.chunked(2).forEach { rowNames ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowNames.forEach { name ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        color = Color(0x1F2C1E14),
                                        shape = RoundedCornerShape(8.dp),
                                        border = androidx.compose.foundation.BorderStroke(0.5.dp, Color(0x3B2C1E14))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "✨ $name",
                                                color = PapyrusTextSecondary,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = PapyrusBg
    )
}