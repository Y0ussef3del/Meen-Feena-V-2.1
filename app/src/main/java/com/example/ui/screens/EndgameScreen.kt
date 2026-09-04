package com.example.ui.screens

import android.app.Activity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.model.GamePhase
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentCard
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*
import com.example.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray

private object PunishmentPoolManager {
    private val remainingIndices = mutableListOf<Int>()

    fun getNextPunishment(
        loserId: String,
        punishmentsList: List<Pair<String, String>>
    ): Pair<String, String> {
        if (punishmentsList.isEmpty()) {
            return Pair("حكم الفرفشة", "يقوم الخاسر بعمل شاي بالنعناع للفائزين ")
        }

        synchronized(this) {
            if (remainingIndices.isEmpty()) {
                remainingIndices.addAll(punishmentsList.indices)
            }

            val randomIndex = kotlin.random.Random.nextInt(remainingIndices.size)
            val selectedIndex = remainingIndices.removeAt(randomIndex)
            return punishmentsList[selectedIndex]
        }
    }
}

@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"
    val context = LocalContext.current

    var showNoInternetDialog by remember { mutableStateOf(false) }

    // قراءة أحكام الخاسرين من ملف res/raw/punishments.json
    val punishmentsList = remember {
        try {
            val inputStream = context.resources.openRawResource(R.raw.punishments)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            (0 until jsonArray.length()).map { i ->
                val obj = jsonArray.getJSONObject(i)
                val type = if (obj.has("type")) obj.getString("type") else if (obj.has("tybe")) obj.getString("tybe") else "حكم"
                val description = obj.optString("description", "")
                Pair(type, description)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val stampScale = remember { Animatable(5f) }
    val stampTranslationY = remember { Animatable(-300f) }
    val stampRotation = remember { Animatable(-55f) }
    val stampAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (state.phase == GamePhase.ENDGAME) {
            MysteryAudioPlayer.playGameOverSound(context, isInnocentsWinner)

            delay(2500)

            launch {
                stampAlpha.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 150)
                )
            }

            launch {
                stampRotation.animateTo(
                    targetValue = -12f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }

            stampTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            stampScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    if (showNoInternetDialog) {
        AlertDialog(
            onDismissRequest = { showNoInternetDialog = false },
            confirmButton = {
                TextButton(onClick = { showNoInternetDialog = false }) {
                    Text("حسناً", color = RedAccent, fontWeight = FontWeight.Bold)
                }
            },
            title = {
                Text(
                    text = "مفيش إنترنت",
                    color = Color(0xFF4A1008),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "برجاء الاتصال بالإنترنت لمواصلة اللعب.",
                    color = PapyrusText,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            containerColor = PapyrusBg,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "جه وقت الحقيقة")
        Spacer(modifier = Modifier.height(14.dp))

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            ParchmentCard(modifier = Modifier.fillMaxSize(), seed = 4441L) {
                Box(modifier = Modifier.size(100.dp).background(Color(0x1FA2A012), CircleShape).border(2.dp, GoldYell, CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy logo endgame", tint = GoldYell, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = if (isInnocentsWinner) "ايه الحلاوة دي الله ينور عليكم" else "المجرم ضحك علي الكل", color = if (isInnocentsWinner) GreenAccent else RedAccent, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("endgame_victory_title"))
                Spacer(modifier = Modifier.height(10.dp))
                Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0C000000), RoundedCornerShape(10.dp)).padding(14.dp)) {
                    LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        item(key = "case_explanation_section") {
                            HorizontalDivider(color = Color(0x3B2C1E14))
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = Color(0x3B2C1E14))
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(text = "ايه اللي حصل بالتفصيل : ", color = Color(0xFF355E3B), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.fillMaxWidth().testTag("case_explanation_header"))
                            Spacer(modifier = Modifier.height(8.dp))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1F2A9D8F)), shape = RoundedCornerShape(8.dp)) {
                                Text(text = currentCase?.explanation ?: "لم تتوفر سجلات سردية للملف.", color = Color(0xFF1D3557), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(12.dp).testTag("case_explanation_text"))
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x3B2C1E14))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(text = "كشف هويات :", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        items(items = state.players, key = { "endgame_player_${it.id}" }) { p ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (p.isMafia) "مجرم" else "بريء ", color = if (p.isMafia) RedAccent else InnocentAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${p.name} (${p.character?.name ?: ""})", color = PapyrusTextSecondary, fontSize = 15.sp)
                            }
                        }

                        // قسم أحكام الخاسرين في أسفل الصفحة
                        item(key = "punishments_header_section") {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color(0x3B2C1E14))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "دلوقتي وقت الحكم يحلو :",
                                color = RedAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                modifier = Modifier.fillMaxWidth().testTag("punishments_header")
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        val losers = state.players.filter { if (isInnocentsWinner) it.isMafia else !it.isMafia }

                        items(items = losers, key = { "endgame_loser_${it.id}" }) { loser ->
                            val punishment = remember(loser.id, state.currentCase, state.winnerSide) {
                                PunishmentPoolManager.getNextPunishment(loser.id, punishmentsList)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0x1F9B2226)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "${loser.name} (${loser.character?.name ?: ""})",
                                            color = RedAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = punishment.first,
                                            color = GoldYell,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            modifier = Modifier
                                                .background(Color(0x3D000000), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = punishment.second,
                                        color = PapyrusText,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            val imageRes = if (isInnocentsWinner) R.drawable.stamp_success else R.drawable.stamp_failed

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Game Result Stamp",
                colorFilter = ColorFilter.tint(Color.Unspecified, blendMode = BlendMode.Screen),
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .aspectRatio(1.8f)
                    .graphicsLayer {
                        scaleX = stampScale.value
                        scaleY = stampScale.value
                        translationY = stampTranslationY.value + 130.dp.toPx()
                        rotationZ = stampRotation.value
                        alpha = stampAlpha.value
                    }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    viewModel.playButtonClick()
                    if (context is Activity) {
                        viewModel.playAgainWithActivity(context) {
                            showNoInternetDialog = true
                        }
                    } else {
                        viewModel.playAgain()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp).testTag("play_again_button"),
                contentPadding = PaddingValues(15.dp)
            ) {
                Icon(Icons.Default.Refresh, "Play again", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                val textLabel = if (state.heartsCount > 0) "لعب جولة وقضية جديدة" else "شاهد إعلان للعب قضية جديدة"
                Text(textLabel, color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            OutlinedButton(
                onClick = {
                    viewModel.playButtonClick()
                    if (context is Activity) {
                        viewModel.showInterstitialAd(context) {
                            viewModel.resetToMainMenu()
                        }
                    } else {
                        viewModel.resetToMainMenu()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Default.Home, "Main menu", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("العودة للقائمة الرئيسية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        }
    }
}