package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
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
    val isHost = state.mode != "ONLINE" || state.hostId == viewModel.myPlayerId.value

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "تفاصيل الجريمة")
            Spacer(modifier = Modifier.height(10.dp))

            ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 10.dp)
                ) {
                    item(key = "case_title") {
                        Text(
                            text = currentCase.title,
                            color = Color(0xFF7A1B0C),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item(key = "case_meta") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    item(key = "case_desc") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x12000000), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = currentCase.description,
                                color = PapyrusText,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                textAlign = TextAlign.Start
                            )
                        }
                    }

                    item(key = "suspects_header") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "المشتبه فيهم :",
                            color = Color(0xFF7A1B0C),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        )
                    }

                    items(
                        items = currentCase.characters,
                        key = { character -> character.name + character.occupation }
                    ) { character ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x08000000), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = character.name + " ( " + character.occupation + " ) ",
                                    color = Color(0xFF7A1B0C),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = character.background,
                                    color = PapyrusText,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { viewModel.startCaseInvestigationIntro() },
                enabled = isHost,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.fillMaxWidth().testTag("case_details_confirm_button"),
                contentPadding = PaddingValues(15.dp)
            ) {
                Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("خش علي الدليل الاول", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
        }
    }
}