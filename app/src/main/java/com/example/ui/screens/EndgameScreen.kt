package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.R // تأكد من مطابقة اسم الباكيج الخاص بمشروعك لملف الـ R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"
    val context = LocalContext.current

    // قيم البداية للأنيميشن (الختم معلق في الهواء، ضخم، مائل ومخفي)
    val stampScale = remember { Animatable(5f) }
    val stampTranslationY = remember { Animatable(-300f) }
    val stampRotation = remember { Animatable(-55f) }
    val stampAlpha = remember { Animatable(0f) }

    LaunchedEffect(isInnocentsWinner, state.phase) {
        if (state.phase == GamePhase.ENDGAME) {
            // 1. شغل صوت النهاية (فوز أو خسارة) فوراً عند دخول الشاشة
            MysteryAudioPlayer.playGameOverSound(context, isInnocentsWinner)

            // 2. انتظر حتى ينتهي الصوت تماماً (مثلاً الصوت مدته ثانيتين ونصف 2500ms)
            // يمكنك زيادة أو تقليل هذا الرقم ليناسب طول ملف الصوت لديك بالظبط
            delay(2500)

            // 3. الآن بعد انتهاء الصوت، ابدأ أنيميشن الختم المتزامن والسريع
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

            // السقوط سينتهي عند نقطة الصفر (التي سنقوم بتزحزيحها لأسفل داخل الـ graphicsLayer)
            stampTranslationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )

            // انكماش الختم للحجم الطبيعي لإعطاء تأثير الارتطام بالورقة
            stampScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
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
                        item {
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

            // اختيار ملف الختم المناسب
            val imageRes = if (isInnocentsWinner) R.drawable.stamp_success else R.drawable.stamp_failed

            Image(
                painter = painterResource(id = imageRes),
                contentDescription = "Game Result Stamp",
                colorFilter = ColorFilter.tint(Color.Unspecified, blendMode = BlendMode.Screen),
                modifier = Modifier
                    .fillMaxWidth(0.55f) // تم تصغير العرض من 0.85f ليكون أصغر وأكثر تناسقاً
                    .aspectRatio(1.8f)
                    .graphicsLayer {
                        scaleX = stampScale.value
                        scaleY = stampScale.value
                        // إزاحة نقطة الاستقرار بمقدار 130dp لأسفل الشاشة لكي يظهر الختم أسفل الكارت
                        // عند السقوط، سيبدأ من (-300 + 130) وينتهي بسلاسة عند (0 + 130)
                        translationY = stampTranslationY.value + 130.dp.toPx()
                        rotationZ = stampRotation.value
                        alpha = stampAlpha.value
                    }
            )
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