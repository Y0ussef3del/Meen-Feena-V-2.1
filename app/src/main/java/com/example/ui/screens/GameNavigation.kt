package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.viewmodel.GamePhase
import com.example.game.viewmodel.RoomState
import com.example.game.viewmodel.Player
import com.example.game.viewmodel.GameCharacter
import com.example.game.viewmodel.Case
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.game.audio.MysteryAudioPlayer
import kotlin.math.cos
import kotlin.math.sin
import com.example.game.model.Case
import com.example.game.model.GamePhase
import com.example.game.model.RoomState
import com.example.game.model.Player
import com.example.game.model.Character

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    var showSplash by remember { mutableStateOf(true) }
    val context = LocalContext.current
    
    LaunchedEffect(Unit) {
        MysteryAudioPlayer.startMusic(context)
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    MysteryBackground(drawBloodDrips = showSplash || state.phase == GamePhase.LOBBY) {
        AnimatedContent(
            targetState = if (showSplash) GamePhase.LOBBY else state.phase,
            transitionSpec = {
                val duration = 800
                (fadeIn(animationSpec = androidx.compose.animation.core.tween(duration)) +
                 slideInVertically(initialOffsetY = { 80 }, animationSpec = androidx.compose.animation.core.tween(duration)) +
                 scaleIn(initialScale = 0.95f, animationSpec = androidx.compose.animation.core.tween(duration))) with
                (fadeOut(animationSpec = androidx.compose.animation.core.tween(500)) +
                 scaleOut(targetScale = 1.05f, animationSpec = androidx.compose.animation.core.tween(500)))
            },
            label = "PhaseTransition"
        ) { phase ->
            if (showSplash) {
                SplashScreen()
            } else {
                when (phase) {
                    GamePhase.LOBBY -> MainMenuOrLobbyScreen(viewModel, state)
                    GamePhase.ROLE_REVEAL -> RoleRevealScreen(viewModel, state)
                    GamePhase.CASE_INTRO -> CaseIntroScreen(viewModel, state)
                    GamePhase.EVIDENCE_ROUND -> EvidenceScreen(viewModel, state)
                    GamePhase.DISCUSSION -> DiscussionScreen(viewModel, state)
                    GamePhase.VOTING -> VotingScreen(viewModel, state)
                    GamePhase.VOTE_RESULT -> VoteResultScreen(viewModel, state)
                    GamePhase.JURY_ROUND -> JuryScreen(viewModel, state)
                    GamePhase.ENDGAME -> EndgameScreen(viewModel, state)
                }
            }
        }
    }
}

@Composable
fun ThrillerTitleComponent(fontSize: androidx.compose.ui.unit.TextUnit = 80.sp) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "مين فينا ؟",
            color = GoldYell,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_logo_arabic")
        )
        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun SplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ThrillerTitleComponent(fontSize = 54.sp)
        Spacer(modifier = Modifier.height(70.dp))
        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = 4.dp,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "الكل متهم .......ولكن ؟",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 30.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun MainMenuOrLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    var showPlayerSetup by remember { mutableStateOf(false) }
    var showLanJoinLobby by remember { mutableStateOf(false) }
    var isSettingsOpen by remember { mutableStateOf(false) }
    
    val discoveredHosts by LanManager.discoveredHosts.collectAsState()
    val localIp = remember { LanManager.getLocalIpAddress() }

    if (isSettingsOpen) {
        SettingsDialog(viewModel = viewModel) { isSettingsOpen = false }
    }

    if (state.mode == "LAN") {
        val isHost = state.hostId == viewModel.myPlayerId.value
        if (isHost) {
            HostLobbyScreen(viewModel, state)
        } else {
            ClientWaitingScreen(viewModel, state)
        }
    } else {
        if (showPlayerSetup) {
            LocalSetupScreen(viewModel, state) { showPlayerSetup = false }
        } else if (showLanJoinLobby) {
            LanJoinLobbyScreen(viewModel, state, discoveredHosts, localIp) { showLanJoinLobby = false }
        } else {
            MainMenuHomeScreen(
                viewModel = viewModel,
                onStartPassPlay = {
                    MysteryAudioPlayer.playClick()
                    viewModel.setupPassAndPlayGame()
                    showPlayerSetup = true
                },
                onOpenLanJoin = {
                    MysteryAudioPlayer.playClick()
                    LanManager.startDiscovery()
                    showLanJoinLobby = true
                },
                onOpenSettings = { 
                    MysteryAudioPlayer.playClick()
                    isSettingsOpen = true 
                }
            )
        }
    }
}

@Composable
fun HostLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "غرفة المضيف")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 38.sp)
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 789L
        ) {
            Text(
                text = "شارك هذا الكود مع أصدقائك للانضمام:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0A05)),
                border = BorderStroke(2.dp, GoldShine),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = state.roomId,
                    color = GoldShine,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 6.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "اللاعبين المنضمون (${state.players.size} ) : ",
                color = Color(0xFF4A1008),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name + if (player.id == state.hostId) " (مضيف)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        if (player.id != state.hostId) {
                            IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                                Icon(Icons.Default.Delete, "Remove Client", tint = RedAccent)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val needed = 4 - state.players.size
            if (needed > 0) {
                Text(
                    text = "متبقي $needed لاعبين كحد أدنى للبدء.",
                    color = RedAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "الغرفة جاهزة لبدء القضية!",
                    color = InnocentAccent,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { viewModel.resetToMainMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1008)),
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء الغرفة", color = GoldShine)
            }
            Button(
                onClick = { viewModel.startInvestigationGame() },
                enabled = state.players.size in 4..6,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.PlayArrow, "Start game", tint = GoldShine)
                Spacer(modifier = Modifier.width(6.dp))
                Text("ابدأ القضية!", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun ClientWaitingScreen(viewModel: GameViewModel, state: RoomState) {
    val myName = viewModel.myPlayerName.collectAsState().value
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "في انتظار التحقيق")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 38.sp)
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 999L
        ) {
            Text(
                text = "أنت منضم للغرفة رقم:",
                color = PapyrusTextSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = state.roomId,
                color = Color(0xFF4A1008),
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 4.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedAccent, strokeWidth = 3.dp)
                Icon(Icons.Default.Fingerprint, "Investigating fingerprints", tint = DarkWoodButton, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "يرجى الانتظار بينما يجمع المضيف اللاعبين الآخرين لبدء توزيع الأدلة الجنائية السرية...",
                color = PapyrusText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = " اللاعبون الحاليون باللوبي (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x06000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x142C1E14), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(if (player.name == myName) RedAccent else DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = player.name + if (player.name == myName) " (أنت)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { viewModel.resetToMainMenu() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("مغادرة والرجوع للرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun LocalSetupScreen(viewModel: GameViewModel, state: RoomState, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempPlayerName by remember { mutableStateOf("") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "إعداد اللاعبين")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 32.sp)
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 123L
        ) {
            Text(
                text = "عدد اللاعبين: ${state.players.size} ",
                color = Color(0xFF4A1008),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "4 - 6 لاعبين (1 مجرم في 4 لاعبين، 2 مجرم في 5+ لاعبين)",
                color = PapyrusTextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = tempPlayerName,
                    onValueChange = { tempPlayerName = it },
                    label = { Text("اسم اللاعب الجديد") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("player_name_input"),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (tempPlayerName.isNotBlank()) {
                            if (state.players.size < 6) {
                                viewModel.addLocalLobbyPlayer(tempPlayerName)
                                tempPlayerName = ""
                            } else {
                                Toast.makeText(context, "الحد الأقصى هو 6 لاعبين", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.testTag("add_player_button")
                ) {
                    Icon(Icons.Default.Add, "Add player", tint = GoldShine)
                }
            }
            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = player.name,
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = RedAccent)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                modifier = Modifier.weight(1f)
            ) {
                Text("رجوع")
            }
            Button(
                onClick = {
                    if (state.players.size < 4) {
                        Toast.makeText(context, "يجب توافر 4 لاعبين كحد أدنى للبدء", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.startInvestigationGame()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier
                    .weight(1.5f)
                    .testTag("start_game_button"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine)
                Spacer(modifier = Modifier.width(8.dp))
                Text("ابدأ اللعبة 🔍", color = GoldShine, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun LanJoinLobbyScreen(
    viewModel: GameViewModel,
    state: RoomState,
    discoveredHosts: Map<String, String>,
    localIp: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var inputCode by remember { mutableStateOf("") }
    var playerNameInput by remember { mutableStateOf("حمادة") }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "الانضمام للغرفة")
        Spacer(modifier = Modifier.height(10.dp))
        ThrillerTitleComponent(fontSize = 32.sp)
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 456L
        ) {
            Text(
                text = "جهازك متصل بالشبكة المحلية IP: $localIp",
                color = PapyrusTextSecondary,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                label = { Text("اسمك في اللعبة") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PapyrusText,
                    unfocusedTextColor = PapyrusText,
                    focusedBorderColor = DarkWoodButton,
                    unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth().testTag("player_name_input"),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "انضم عن طريق رمز الغرفة:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = { 
                        if (it.length <= 5) {
                            inputCode = it.filter { char -> char.isDigit() }
                        }
                    },
                    label = { Text("اكتب الرمز (5 أرقام)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = PapyrusText,
                        unfocusedTextColor = PapyrusText,
                        focusedBorderColor = DarkWoodButton,
                        unfocusedBorderColor = PapyrusTextSecondary.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.weight(1.5f),
                    singleLine = true
                )
                Button(
                    onClick = {
                        if (inputCode.length == 5) {
                            val success = viewModel.joinLanHostByCode(inputCode, playerNameInput)
                            if (!success) {
                                Toast.makeText(
                                    context, 
                                    "يبدو أن الرمز غير نشط بالشبكة حالياً. تأكد من تشغيل الغرفة من المضيف.", 
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        } else {
                            Toast.makeText(context, "الرمز يجب أن يتكون من 5 أرقام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ربط", color = GoldShine)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "أو اختر غرفة من كشف الشبكة (UDP):",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(6.dp))

            if (discoveredHosts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "يبحث عن لغز نشط على الـ WiFi...",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    discoveredHosts.forEach { (ip, hostDetails) ->
                        val parts = hostDetails.split("|")
                        val hostName = parts.getOrNull(0) ?: "غرفة مجهولة"
                        val rCode = parts.getOrNull(1) ?: "----"
                        
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(2.dp, GoldYell, RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.joinLanHost(ip, playerNameInput)
                                    }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold)
                                    Text("رمز الغرفة: $rCode | IP: $ip", color = PapyrusTextSecondary, fontSize = 11.sp)
                                }
                                Icon(Icons.Default.ArrowForward, "Join details", tint = DarkWoodButton)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                LanManager.stopDiscovery()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("الرجوع للقائمة الرئيسية", color = GoldShine)
        }
    }
}

@Composable
fun MainMenuHomeScreen(
    viewModel: GameViewModel,
    onStartPassPlay: () -> Unit,
    onOpenLanJoin: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ThrillerTitleComponent()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onStartPassPlay() }
                    .testTag("new_game_opt_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.PhoneAndroid,
                        contentDescription = "Pass device",
                        tint = DarkWoodButton,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "لعبة جديدة",
                            color = Color(0xFF4A1008),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "جرِّب اللعب بالتمرير (جهاز واحد)",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go play",
                        tint = DarkWoodButton
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onOpenLanJoin() }
                    .testTag("lan_multiplayer_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "WiFi game",
                        tint = DarkWoodButton,
                        modifier = Modifier.size(36.dp)
                    )
                    Column(
                        modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            "دخول برمز الغرفة (WiFi)",
                            color = Color(0xFF4A1008),
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "العب على جهازك مع أصدقائك بالرمز",
                            color = PapyrusTextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go LAN Connect",
                        tint = DarkWoodButton
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable {
                        MysteryAudioPlayer.playClick()
                        viewModel.startLanHost("مضيف التحقيق")
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF35120D)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, GoldYell)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddBox, "Host Game", tint = GoldShine)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "عمل اوضة جديدة (Host)",
                        color = GoldShine,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(12.dp))
                    .clickable { onOpenSettings() }
                    .testTag("settings_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(2.dp, DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Settings, "Config preferences", tint = DarkWoodButton, modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 16.dp), horizontalAlignment = Alignment.End) {
                        Text(
                            "الإعدادات وقواعد اللعب",
                            color = Color(0xFF4A1008),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Go settings",
                        tint = DarkWoodButton
                    )
                }
            }
        }
        Text(
            text = " !! القاعدة الاولي والاخيرة ... شك في الجميع",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 20.dp)
        )
    }
}

@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    var revealed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }
    val char = activePassPlayer.character ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الملفات السرية")
        Spacer(modifier = Modifier.height(10.dp))
        
        if (!revealed) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .background(Color(0x3B6E1C11), CircleShape)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VisibilityOff,
                        contentDescription = "Hide role cards",
                        tint = GoldShine,
                        modifier = Modifier.size(56.dp)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "دور المحقق : ${activePassPlayer.name}",
                    color = PapyrusBgLight,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "اديله التلفون.....وخلي بالك لحد يشوف ",
                    color = Color.LightGray,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
            Button(
                onClick = { 
                    MysteryAudioPlayer.playReveal()
                    revealed = true 
                },
                colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("اضغط لفتح الملف 🔍", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        } else {
            ParchmentCard(
                modifier = Modifier.weight(1f),
                seed = 112233L
            ) {
                Text(
                    text = "ملف الشخصية: ${char.name}",
                    color = Color(0xFF4A1008),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "الوظيفة الخلفية: ${char.occupation}", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "الصفات الشخصية: ${char.traits}", color = PapyrusTextSecondary, fontSize = 15.sp, fontStyle = FontStyle.Italic)
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0x3B2C1E14), thickness = 1.dp)
                Spacer(modifier = Modifier.height(10.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (activePassPlayer.isMafia) RedAccent else InnocentAccent)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security,
                        contentDescription = "Role Symbol",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (activePassPlayer.isMafia) "أنت : المجرم الحقيقي" else "أنت : بريء من الجريمة",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "دافعك المستخبي:",
                    color = Color(0xFF4A1008),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text =char.hiddenMotive,
                    color = PapyrusText,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { 
                    MysteryAudioPlayer.playClick()
                    viewModel.confirmSecretsRevealed()
                    revealed = false 
                },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(54.dp)
            ) {
                Text("فهمت اقفل الملف 🔐", color = GoldShine, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "ملف القضية الجنائية 📄")
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 5544L
        ) {
            Text(
                text = "عنوان القضية: ${currentCase.title}",
                color = Color(0xFF6E1B10),
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0x06000000), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0x142C1E14), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                LazyColumn {
                    item {
                        Text(
                            text = currentCase.description,
                            color = PapyrusText,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = " المشتبه فيهم : ",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                currentCase.characters.forEach { char ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF8C2012), RoundedCornerShape(6.dp))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char.name.split(" ").firstOrNull() ?: char.name,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                MysteryAudioPlayer.playSuccess()
                viewModel.startCaseInvestigationIntro() 
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("case_details_confirm_button"),
            contentPadding = PaddingValues(15.dp)
        ) {
            Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        }
    }
}

@Composable
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentClue = state.currentCase?.description ?: "لا توجد أدلة إضافية متاحة حالياً."
    val hintText = "راقب تصرفات الجميع جيدا ودوافعهم السريّة."
    var showHint by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "تقرير المعمل الجنائي 🧪")
        Spacer(modifier = Modifier.height(10.dp))

        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 4455L
        ) {
            Text(
                text = "الدليل الجنائي:",
                color = Color(0xFF6E1B10),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0x0A6E1B10), RoundedCornerShape(10.dp))
                    .border(2.dp, RedAccent.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentClue,
                    color = PapyrusText,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            
            if (showHint) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3CD)),
                    border = BorderStroke(1.dp, Color(0xFFFFEBAA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 تلميح ..يارب تفهم: $hintText",
                        modifier = Modifier.padding(10.dp),
                        color = Color(0xFF856404),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (!showHint) {
                Button(
                    onClick = { 
                        MysteryAudioPlayer.playWarning()
                        showHint = true 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A012)),
                    modifier = Modifier.testTag("clue_hint_button")
                ) {
                    Icon(Icons.Default.Warning, "Clues Alert", tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("عرض تلميح 💡", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { 
                MysteryAudioPlayer.playClick()
                viewModel.advanceFromEvidenceToDiscussion() 
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("evidence_reveal_advance"),
            contentPadding = PaddingValues(15.dp)
        ) {
            Icon(Icons.Default.RecordVoiceOver, "Discuss", tint = GoldShine)
            Spacer(modifier = Modifier.width(8.dp))
            Text("فتح طاولة النقاش والمواجهة 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val suspectedByClick = remember { mutableStateListOf<String>() }
    
    // Dynamic countdown timer based on user customized settings (defaults to 2 mins if not configured)
    val configuredMins = state.discussionDurationMins
    val totalSeconds = configuredMins * 60
    var timerLeft by remember { mutableStateOf(totalSeconds) }

    LaunchedEffect(configuredMins) {
        timerLeft = configuredMins * 60
    }

    LaunchedEffect(timerLeft) {
        if (timerLeft > 0) {
            kotlinx.coroutines.delay(1000)
            timerLeft--
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "طاولة الاستجواب والنقاش 🗣️")
        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1.2f),
            contentAlignment = Alignment.Center
        ) {
            val progress = if (totalSeconds > 0) timerLeft.toFloat() / totalSeconds.toFloat() else 1f
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(color = Color(0x1F000000), style = Stroke(width = 8.dp.toPx()))
                drawArc(
                    color = if (progress < 0.2f) Color.Red else Color.Yellow,
                    startAngle = -90f,
                    sweepAngle = progress * 360f,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val mins = timerLeft / 60
                val secs = timerLeft % 60
                Text(
                    text = String.format("%02d:%02d", mins, secs),
                    color = Color.Yellow,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = HandjetFontFamily
                )
                Text(
                    text = "تبادلوا الشكوك والاتهامات",
                    color = Color.LightGray.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }

            val radius = 130.dp
            state.players.forEachIndexed { idx, player ->
                val angleRad = (idx.toDouble() * (2.0 * Math.PI / state.players.size.toDouble())) - (Math.PI / 2.0)
                val xOffset = (radius.value * cos(angleRad)).dp
                val yOffset = (radius.value * sin(angleRad)).dp

                val isClickSuspected = suspectedByClick.contains(player.id)

                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(68.dp)
                        .shadow(3.dp, CircleShape)
                        .background(
                            if (isClickSuspected) Color(0xFFC42512) else Color(0xFF421E14), CircleShape
                        )
                        .border(
                            2.dp, if (isClickSuspected) Color.Yellow else Color(0x3BFFFFFF), CircleShape
                        )
                        .clickable {
                            MysteryAudioPlayer.playClick()
                            if (isClickSuspected) {
                                suspectedByClick.remove(player.id)
                            } else {
                                suspectedByClick.add(player.id)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(4.dp)
                    ) {
                        Text(
                            text = player.name.take(6),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    if (player.isMafia && isClickSuspected) Color.Yellow else Color(0x3B000000), RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال",
                                color = if (isClickSuspected) Color.Black else Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        ParchmentCard(
            modifier = Modifier.wrapContentHeight(),
            seed = 771L
        ) {
            Text(
                text = "ابدأوا فقرة المناقشة والاتهامات ..وبالله عليكم شغلوا دماغكم شوية!",
                color = PapyrusText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Button(
            onClick = { 
                MysteryAudioPlayer.playWarning()
                // Automatically route to Jury screen if it's the final round suspects match
                val aliveCount = state.players.count { it.isAlive }
                if (aliveCount <= 2) {
                    viewModel.advanceToJuryRound()
                } else {
                    viewModel.advanceFromDiscussionToVoting() 
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("discussion_advance_button"),
            contentPadding = PaddingValues(15.dp)
        ) {
            val aliveCount = state.players.count { it.isAlive }
            Icon(if (aliveCount <= 2) Icons.Default.Gavel else Icons.Default.Ballot, "Next Frame", tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (aliveCount <= 2) "جولة المحلفين الختامية (المستبعدين) ⚖️" else "الذهاب لصندوق التصويت السري 🗳️", 
                color = Color.White, 
                fontWeight = FontWeight.ExtraBold, 
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val localVoterId = viewModel.myPlayerId.value
    val localVoter = state.players.find { it.id == localVoterId }

    if (state.mode == "LAN") {
        if (localVoter != null && !localVoter.isAlive) {
            MysteryBackground {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "أنت ميت 💀")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "الف مبرووك انك خرجت اقعد جنب اخواتك .....يا فاشل",
                        color = PapyrusBgLight,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        } else if (state.votes.containsKey(localVoterId)) {
            MysteryBackground {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "تم تسجيل صوتك بنجاح! 🗳️")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "مستنيين باقي اللعيبة يصوتوا...",
                        color = PapyrusBgLight,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        }
    }

    val activePlayers = state.players.filter { it.isAlive }
    val voterPlayer = if (state.mode == "PASS_AND_PLAY") {
        activePlayers.getOrNull(state.activePassPlayerIndex)
    } else {
        localVoter
    }

    if (voterPlayer == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("تحميل تصويت المحققين...", color = GoldShine)
        }
        return
    }

    var selectedTargetId by remember { mutableStateOf("") }
    
    val eligibleCandidates = remember(state.votes, state.tiedVotePlayers, voterPlayer.id, state.players) {
        val baseFiltered = if (state.tiedVotePlayers.isNotEmpty()) {
            state.players.filter { it.id in state.tiedVotePlayers }
        } else {
            state.players.filter { it.isAlive }
        }
        baseFiltered.filter { it.id != voterPlayer.id }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(modifier = Modifier.weight(1f), seed = 33L) {
            Text(
                text = "دور اللاعب: ${voterPlayer.name}",
                color = Color(0xFF6E1B10),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "اختار الشخص اللي شاكك فيه ان هو المجرم:",
                color = PapyrusTextSecondary,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(eligibleCandidates) { candidate ->
                    val isSelected = candidate.id == selectedTargetId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp)
                            )
                            .border(
                                2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                MysteryAudioPlayer.playClick()
                                selectedTargetId = candidate.id 
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person,
                                contentDescription = "Pick status target",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            candidate.character?.let {
                                Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                if (selectedTargetId.isNotEmpty()) {
                    MysteryAudioPlayer.playSuccess()
                    viewModel.submitVote(selectedTargetId)
                    selectedTargetId = ""
                }
            },
            enabled = selectedTargetId.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("submit_vote_button")
        ) {
            Text("تأكيد وتسجيل صوتي 🗳️", color = GoldShine, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.mode == "PASS_AND_PLAY" || state.hostId == viewModel.myPlayerId.value
    val eliminated = state.lastEliminatedPlayer

    MysteryBackground {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "نتائج التصويت والعدالة")

            ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (state.tiedVotePlayers.isNotEmpty()) {
                        Text("⚖️ تعادل في الأصوات بين:", fontSize = 22.sp, color = Color(0xFF4A1008), fontWeight = FontWeight.Bold)
                        state.players.filter { it.id in state.tiedVotePlayers }.forEach {
                            Text("• ${it.name}", fontSize = 18.sp, color = RedAccent)
                        }
                    } else if (eliminated != null) {
                        Text("تم استبعاد المشتبه به: ${eliminated.name}", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A1008))
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val identityText = if (eliminated.isMafia) "بطاقة الهوية: 🟥 مجرم !" else "بطاقة الهوية: 🟩  بريء"
                        val identityColor = if (eliminated.isMafia) RedAccent else Color(0xFF2E7D32)
                        
                        Text(text = identityText, fontSize = 22.sp, fontWeight = FontWeight.Black, color = identityColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "الشخصية الملعونة: ${eliminated.character?.name ?: "غير معروف"}", color = PapyrusTextSecondary, fontSize = 16.sp)
                    } else {
                        Text("لم يتم طرد أي أحد في هذه الجولة.", color = PapyrusText)
                    }
                }
            }

            if (isHost) {
                Button(
                    onClick = { viewModel.confirmVoteResultAndProceed() },
                    colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("متابعة التحقيق الجنائي", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val localId = viewModel.myPlayerId.value
    val localPlayer = state.players.find { it.id == localId }

    // CRITICAL FIX: The Jury consists strictly of all ELIMINATED players
    val deadPlayers = state.players.filter { !it.isAlive }
    
    // The pool of remaining suspects to vote against
    val finalTwoSuspects = state.players.filter { it.isAlive }

    val juryVoter = if (state.mode == "PASS_AND_PLAY") {
        deadPlayers.getOrNull(state.activePassPlayerIndex)
    } else {
        localPlayer
    }

    if (state.mode == "LAN") {
        if (localPlayer != null && localPlayer.isAlive) {
            MysteryBackground {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    ParchmentHeaderBanner(text = "مصيرك معلق بأصوات المحلفين ! ⚖️")
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "أنت أحد المشتبه بهم النهائيين! اللاعبين المستبعدين يتناقشون ويصوتون ضد أحدكم الآن لحسم القضية...",
                        color = PapyrusBgLight,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return
        } else {
            val hasVoted = state.juryVotes.containsKey(localId)
            if (hasVoted) {
                MysteryBackground {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp).safeDrawingPadding(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ParchmentHeaderBanner(text = "تم تسجيل صوتك للمحلفين! ⚖️")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "مستنيين باقي اللاعبين المستبعدين يخلصوا تصويت عشان تظهر النتيجة الكاملة...",
                            color = PapyrusBgLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                return
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "محكمة المحلفين (المستبعدين) ⚖️")
        Spacer(modifier = Modifier.height(10.dp))
        
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 88L
        ) {
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .background(Color(0x3B6E1B10), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Gavel, "Gavel judge", tint = RedAccent, modifier = Modifier.size(38.dp))
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "جولة الحسم النهائي!",
                color = Color(0xFF6E1D10),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "كل اللاعبين اللي خرجوا مطلوب منكم دلوقتي تحددوا بين الاتنين دول مين المجرم الحقيقي ..ركزوا بالله عليكم",
                color = PapyrusText,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(12.dp))
            
            if (juryVoter != null) {
                Text(
                    text = "دور اللاعب المحلف الحالي : ${juryVoter.name}",
                    color = RedAccent,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                var selectedTargetId by remember { mutableStateOf("") }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(finalTwoSuspects) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0x2B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp))
                                .border(2.dp, if (isSelected) RedAccent else Color(0x11000000), RoundedCornerShape(10.dp))
                                .clickable {
                                    MysteryAudioPlayer.playClick()
                                    selectedTargetId = candidate.id
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = isSelected, onClick = { selectedTargetId = candidate.id })
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("الشخصية السرية: ${candidate.character?.name ?: ""}", color = PapyrusTextSecondary, fontSize = 12.sp)
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        if (selectedTargetId.isNotEmpty()) {
                            MysteryAudioPlayer.playSuccess()
                            viewModel.submitJuryVote(selectedTargetId)
                            selectedTargetId = ""
                        }
                    },
                    enabled = selectedTargetId.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("تسجيل صوت الإدانة النهائي ⚖️", color = GoldShine)
                }
            } else {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RedAccent)
                }
            }
        }
    }
}

@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    
    MysteryBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .safeDrawingPadding()
                .verticalScroll(androidx.compose.foundation.rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "إغلاق السجلات وحل اللغز 🏆")
            Spacer(modifier = Modifier.height(14.dp))

            ParchmentCard(
                modifier = Modifier.fillMaxWidth(),
                seed = 9911L
            ) {
                Text(
                    text = if (state.winnerSide == "MAFIA") "🔥 برافو يبلدينا برافو ,المجرم غفلكوا كلكو!" else "🕊️ الله ينور عليكم يشباب عرفتوا توقعوا المجرم!",
                    color = if (state.winnerSide == "MAFIA") RedAccent else InnocentAccent,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                
                Text(
                    text = "الحقيقة الكاملة وراء الكواليس:",
                    color = Color(0xFF6E1B10),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0x1F6E1B10)),
                    border = BorderStroke(1.dp, Color(0x3B6E1B10)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = currentCase?.explanation ?: "لم تتوفر سجلات سردية للملف.",
                        color = Color(0xFF1D3557),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        modifier = Modifier.padding(12.dp).testTag("case_explanation_text")
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x3B2C1E14))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "كشف هويات كل اللاعبين بغرفة التحقيق:",
                    color = DarkWoodButton,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                state.players.forEach { p ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (p.isMafia) "مجرم" else "بريء ",
                            color = if (p.isMafia) RedAccent else InnocentAccent,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "${p.name} (${p.character?.name ?: ""})",
                            color = PapyrusTextSecondary,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        viewModel.playAgain() 
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("play_again_button"),
                ) {
                    Text("لعب جولة جديدة 🔄", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                
                OutlinedButton(
                    onClick = { 
                        MysteryAudioPlayer.playClick()
                        viewModel.resetToMainMenu() 
                    },
                    border = BorderStroke(2.dp, DarkWoodButton),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("الخروج للقائمة الرئيسية", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismiss: () -> Unit) {
    // Read the settings from the View Model's Flow
    val state by viewModel.roomState.collectAsState()
    val durationMins = state.settings.discussionTimeMinutes

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
            ) {
                Text("حفظ وإغلاق", color = GoldShine)
            }
        },
        title = {
            Text(
                text = "إعدادات وقواعد اللعب",
                color = Color(0xFF4A1008),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            ParchmentCard(
                seed = 77L,
                contentPadding = PaddingValues(12.dp),
                modifier = Modifier.wrapContentHeight()
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "مدة جولة النقاش: $durationMins دقيقة",
                            color = PapyrusText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                // Explicitly update passing all config params over
                                onClick = { 
                                    if (durationMins > 1) viewModel.updateSettings(durationMins - 1, state.settings.votingTimeMinutes, state.settings.isMusicEnabled, state.settings.volume) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                                modifier = Modifier.weight(1f)
                            ) { Text("-1 دقيقة", color = GoldShine) }
                            
                            Button(
                                onClick = { 
                                    if (durationMins < 10) viewModel.updateSettings(durationMins + 1, state.settings.votingTimeMinutes, state.settings.isMusicEnabled, state.settings.volume) 
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                                modifier = Modifier.weight(1f)
                            ) { Text("+1 دقيقة", color = GoldShine) }
                        }
                    }
                }
            }
        }
    )
}