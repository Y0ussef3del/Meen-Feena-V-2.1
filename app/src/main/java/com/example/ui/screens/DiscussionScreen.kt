package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.game.model.GamePhase
import com.example.game.model.RoomState
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.ParchmentHeaderBanner
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val suspectedByClick = remember { mutableStateListOf<String>() }

    val alivePlayers = remember(state.players) {
        state.players.filter { it.isAlive }
    }

    val isHost = state.mode != "ONLINE" || state.hostId == viewModel.myPlayerId.value

    val isMicMuted by viewModel.isMicMuted.collectAsState()
    val mutedPlayers by viewModel.mutedPlayersState.collectAsState()
    val voiceStatusText by viewModel.voiceChatStatus.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceChatIfPermitted()
        }
    }

    LaunchedEffect(state.mode, state.phase, state.players) {
        if (state.mode == "ONLINE" && state.phase == GamePhase.DISCUSSION) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                viewModel.startVoiceChatIfPermitted()
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    if (alivePlayers.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("لا يوجد لاعبين أحياء حالياً في الغرفة", color = Color.White)
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .navigationBarsPadding()
            .statusBarsPadding()
    ) {
        val minSide = minOf(maxWidth, maxHeight)
        val timerSize = remember(minSide) { (minSide * 0.34f).coerceIn(120.dp, 170.dp) }
        val avatarSize = remember(minSide) { (minSide * 0.14f).coerceIn(52.dp, 68.dp) }
        val radius = remember(minSide) { (minSide * 0.32f).coerceIn(90.dp, 150.dp) }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                ParchmentHeaderBanner(text = "ده وقت النقاش والمواجهة")

                if (state.mode == "ONLINE") {
                    IconButton(
                        onClick = { viewModel.toggleSelfMic() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 8.dp)
                            .size(42.dp)
                            .background(if (isMicMuted) RedAccent else DarkWoodButton, CircleShape)
                            .border(1.5.dp, GoldShine, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isMicMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            contentDescription = "Mute Unmute Mic",
                            tint = GoldShine,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // شريط توضيح حالة الاتصال بالصوت والأخطاء للتأكد من الحالة وحل أي مشكلة فورية
            if (state.mode == "ONLINE") {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = Color(0x992C0A05),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldShine.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Status Info",
                            tint = GoldShine,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = voiceStatusText,
                            color = Color.White,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                val formattedTime = remember(state.timerSecondsLeft) {
                    String.format("%02d:%02d", state.timerSecondsLeft / 60, state.timerSecondsLeft % 60)
                }

                Canvas(modifier = Modifier.size(timerSize)) {
                    drawCircle(color = Color(0xFF1E0604), radius = size.minDimension / 2)
                    val sweepAngle = if (state.timerTotalSeconds > 0) {
                        (state.timerSecondsLeft.toFloat() / state.timerTotalSeconds.toFloat()) * 360f
                    } else 360f

                    drawArc(
                        color = Color(0xFFE73224),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("فاضل", color = GoldYell, fontSize = 12.sp)
                    Text(
                        text = formattedTime,
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.testTag("timer_countdown_display")
                    )
                    Text("للإدلاء بالاستنتاج", color = PapyrusBgLight.copy(alpha = 0.5f), fontSize = 10.sp)
                }

                val playerCount = alivePlayers.size.coerceAtLeast(1)

                alivePlayers.forEachIndexed { index, player ->
                    val angleInDegrees = (index * (360.0 / playerCount)) - 90.0
                    val angleInRadians = Math.toRadians(angleInDegrees)
                    val offsetX = (radius.value * cos(angleInRadians)).dp
                    val offsetY = (radius.value * sin(angleInRadians)).dp

                    val isSuspected = player.id in suspectedByClick
                    val isRemotePlayerMuted = mutedPlayers.contains(player.id)

                    Box(
                        modifier = Modifier
                            .offset(x = offsetX, y = offsetY)
                            .size(avatarSize)
                            .clickable {
                                if (isSuspected) {
                                    suspectedByClick.remove(player.id)
                                } else {
                                    suspectedByClick.add(player.id)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isSuspected) RedAccent else DarkWoodButton,
                            border = androidx.compose.foundation.BorderStroke(
                                2.dp,
                                if (isSuspected) GoldYell else GoldShine
                            ),
                            modifier = Modifier
                                .fillMaxSize()
                                .shadow(4.dp, CircleShape)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Player Avatar",
                                    tint = GoldShine,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = player.name,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        if (state.mode == "ONLINE" && player.id != viewModel.myPlayerId.value) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 4.dp, y = (-4).dp)
                                    .size(24.dp)
                                    .background(
                                        if (isRemotePlayerMuted) RedAccent else Color(0xCC2C0A05),
                                        CircleShape
                                    )
                                    .border(1.dp, GoldShine, CircleShape)
                                    .clickable { viewModel.toggleMutePlayer(player.id) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isRemotePlayerMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Mute Remote Player",
                                    tint = Color.White,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isHost) {
                Button(
                    onClick = { viewModel.advanceFromDiscussionToVoting() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("end_discussion_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.HowToVote, "Vote now", tint = GoldShine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("الانتقال للتصويت 🗳️", color = GoldShine, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F2C1E14)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "في انتظار المضيف للانتقال للتصويت...",
                        color = GoldShine,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(14.dp)
                    )
                }
            }
        }
    }
}