package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.game.model.*
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

// استيراد دوال المساعدة المتجاوبة
import com.example.ui.components.scaledDp
import com.example.ui.components.scaledSp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val context = LocalContext.current

    var showCompanySplash by remember { mutableStateOf(true) }
    var showOldSplash by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        showCompanySplash = false
        showOldSplash = true
    }

    MysteryBackground(drawBloodDrips = showCompanySplash || showOldSplash || state.phase == GamePhase.LOBBY) {
        when {
            showCompanySplash -> CompanySplashScreen()
            showOldSplash -> OldSplashScreen(onFinished = { showOldSplash = false })
            else -> {
                AnimatedContent(
                    targetState = state.phase,
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
}

@Composable
fun CompanySplashScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(24)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(scaledDp(180))
                .shadow(scaledDp(8), shape = CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0A05)),
            border = BorderStroke(scaledDp(2), GoldShine)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Company Logo",
                    tint = GoldYell,
                    modifier = Modifier.size(scaledDp(80))
                )
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(24)))
        Text(
            text = "Mystery Games",
            color = GoldYell,
            fontSize = scaledSp(32),
            fontWeight = FontWeight.Bold,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(scaledDp(12)))
        Text(
            text = "Investigative Thrillers",
            color = PapyrusBgLight.copy(alpha = 0.7f),
            fontSize = scaledSp(16),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(scaledDp(40)))
        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = scaledDp(3).value,
            modifier = Modifier.size(scaledDp(36))
        )
    }
}

@Composable
fun OldSplashScreen(onFinished: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        onFinished()
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(24)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        ThrillerTitleComponent(fontSize = scaledSp(54))
        Spacer(modifier = Modifier.height(scaledDp(70)))
        CircularProgressIndicator(
            color = RedAccent,
            strokeWidth = scaledDp(4).value,
            modifier = Modifier.size(scaledDp(48))
        )
        Spacer(modifier = Modifier.height(scaledDp(24)))
        Text(
            text = "الكل متهم .......ولكن ؟",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = scaledSp(30),
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ThrillerTitleComponent(fontSize: androidx.compose.ui.unit.TextUnit? = null) {
    val effectiveFontSize = fontSize ?: scaledSp(80)
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(scaledDp(16))
    ) {
        Spacer(modifier = Modifier.height(scaledDp(12)))
        Text(
            text = "مين فينا ؟",
            color = GoldYell,
            fontSize = effectiveFontSize,
            fontWeight = FontWeight.Black,
            fontFamily = HandjetFontFamily,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag("app_logo_arabic")
        )
        Spacer(modifier = Modifier.height(scaledDp(4)))
    }
}

@Composable
fun MainMenuOrLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
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
                    viewModel.setupPassAndPlayGame()
                    showPlayerSetup = true
                },
                onOpenLanJoin = {
                    LanManager.startDiscovery()
                    showLanJoinLobby = true
                },
                onOpenSettings = { isSettingsOpen = true }
            )
        }
    }
}

@Composable
fun HostLobbyScreen(viewModel: GameViewModel, state: RoomState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "غرفة المضيف")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ThrillerTitleComponent(fontSize = scaledSp(38))
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 789L
        ) {
            Text(
                text = "شارك هذا الكود مع أصدقائك للانضمام:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(15),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(scaledDp(6)))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C0A05)),
                border = BorderStroke(scaledDp(2), GoldShine),
                shape = RoundedCornerShape(scaledDp(12))
            ) {
                Text(
                    text = state.roomId,
                    color = GoldShine,
                    fontSize = scaledSp(34),
                    fontWeight = FontWeight.Black,
                    letterSpacing = scaledSp(6),
                    modifier = Modifier.padding(horizontal = scaledDp(24), vertical = scaledDp(8)),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(scaledDp(16)))
            Text(
                text = "اللاعبين المنضمون (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = scaledSp(16),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(scaledDp(8)))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(scaledDp(8))
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(scaledDp(8)))
                            .border(scaledDp(1), Color(0x1F2C1E14), RoundedCornerShape(scaledDp(8)))
                            .padding(scaledDp(12)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(scaledDp(36))
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = scaledSp(14)
                            )
                        }
                        Spacer(modifier = Modifier.width(scaledDp(12)))
                        Text(
                            text = player.name + if (player.id == state.hostId) " (مضيف)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = scaledSp(14),
                            modifier = Modifier.weight(1f)
                        )
                        if (player.id != state.hostId) {
                            IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                                Icon(Icons.Default.Delete, "Remove Client", tint = RedAccent, modifier = Modifier.size(scaledDp(24)))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            val needed = 4 - state.players.size
            if (needed > 0) {
                Text(
                    text = "متبقي $needed لاعبين كحد أدنى للبدء.",
                    color = RedAccent,
                    fontSize = scaledSp(13),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "الغرفة جاهزة لبدء القضية!",
                    color = InnocentAccent,
                    fontSize = scaledSp(13),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(12))
        ) {
            Button(
                onClick = { viewModel.resetToMainMenu() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1008)),
                modifier = Modifier.weight(1f)
            ) {
                Text("إلغاء الغرفة", color = GoldShine, fontSize = scaledSp(14))
            }
            Button(
                onClick = { viewModel.startInvestigationGame() },
                enabled = state.players.size in 4..6,
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                modifier = Modifier.weight(1.5f)
            ) {
                Icon(Icons.Default.PlayArrow, "Start game", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                Spacer(modifier = Modifier.width(scaledDp(6)))
                Text("ابدأ القضية!", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
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
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "في انتظار التحقيق")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ThrillerTitleComponent(fontSize = scaledSp(38))
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 999L
        ) {
            Text(
                text = "أنت منضم للغرفة رقم:",
                color = PapyrusTextSecondary,
                fontSize = scaledSp(14),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(scaledDp(4)))
            Text(
                text = state.roomId,
                color = Color(0xFF4A1008),
                fontSize = scaledSp(30),
                fontWeight = FontWeight.Black,
                letterSpacing = scaledSp(4),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(scaledDp(12)))
            Box(
                modifier = Modifier.size(scaledDp(60)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = RedAccent, strokeWidth = scaledDp(3).value)
                Icon(Icons.Default.Fingerprint, "Investigating fingerprints", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(32)))
            }
            Spacer(modifier = Modifier.height(scaledDp(12)))
            Text(
                text = "يرجى الانتظار بينما يجمع المضيف اللاعبين الآخرين لبدء توزيع الأدلة الجنائية السرية...",
                color = PapyrusText,
                fontSize = scaledSp(14),
                lineHeight = scaledSp(20),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(scaledDp(16)))
            Text(
                text = " اللاعبون الحاليون باللوبي (${state.players.size}) : ",
                color = Color(0xFF4A1008),
                fontSize = scaledSp(15),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(scaledDp(6)))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(scaledDp(6))
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x06000000), RoundedCornerShape(scaledDp(8)))
                            .border(scaledDp(1), Color(0x142C1E14), RoundedCornerShape(scaledDp(8)))
                            .padding(scaledDp(10)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(scaledDp(28))
                                .background(if (player.name == myName) RedAccent else DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = scaledSp(12)
                            )
                        }
                        Spacer(modifier = Modifier.width(scaledDp(10)))
                        Text(
                            text = player.name + if (player.name == myName) " (أنت)" else "",
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = scaledSp(14)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(14)))
        Button(
            onClick = { viewModel.resetToMainMenu() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("مغادرة والرجوع للرئيسية", color = GoldShine, fontSize = scaledSp(14))
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
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "إعداد اللاعبين")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ThrillerTitleComponent(fontSize = scaledSp(32))
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 123L
        ) {
            Text(
                text = "عدد اللاعبين: ${state.players.size} ",
                color = Color(0xFF4A1008),
                fontSize = scaledSp(18),
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "4 - 6 لاعبين (1 مجرم في 4 لاعبين، 2 مجرم في 5+ لاعبين)",
                color = PapyrusTextSecondary,
                fontSize = scaledSp(12),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8))
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
                    modifier = Modifier.weight(1f).testTag("player_name_input"),
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = scaledSp(14))
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
                    Icon(Icons.Default.Add, "Add player", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(14)))
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(scaledDp(8))
            ) {
                items(state.players) { player ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0x0C000000), RoundedCornerShape(scaledDp(8)))
                            .border(scaledDp(1), Color(0x1F2C1E14), RoundedCornerShape(scaledDp(8)))
                            .padding(scaledDp(12)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(scaledDp(36))
                                .background(DarkWoodButton, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = player.avatarId.toString(),
                                color = GoldShine,
                                fontWeight = FontWeight.Bold,
                                fontSize = scaledSp(14)
                            )
                        }
                        Spacer(modifier = Modifier.width(scaledDp(12)))
                        Text(
                            text = player.name,
                            color = PapyrusText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = scaledSp(14),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removePlayerFromLobby(player.id) }) {
                            Icon(Icons.Default.Delete, "Remove", tint = RedAccent, modifier = Modifier.size(scaledDp(24)))
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(scaledDp(12))
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine),
                modifier = Modifier.weight(1f)
            ) {
                Text("رجوع", fontSize = scaledSp(14))
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
                modifier = Modifier.weight(1.5f).testTag("start_game_button"),
                contentPadding = PaddingValues(scaledDp(16))
            ) {
                Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                Spacer(modifier = Modifier.width(scaledDp(8)))
                Text("ابدأ اللعبة 🔍", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
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
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ParchmentHeaderBanner(text = "الانضمام للغرفة")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ThrillerTitleComponent(fontSize = scaledSp(32))
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(
            modifier = Modifier.weight(1f),
            seed = 456L
        ) {
            Text(
                text = "جهازك متصل بالشبكة المحلية IP: $localIp",
                color = PapyrusTextSecondary,
                fontSize = scaledSp(12)
            )
            Spacer(modifier = Modifier.height(scaledDp(12)))
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
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(fontSize = scaledSp(14))
            )
            Spacer(modifier = Modifier.height(scaledDp(14)))
            Text(
                text = "انضم عن طريق رمز الغرفة:",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(14)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(scaledDp(8))
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
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(fontSize = scaledSp(14))
                )
                Button(
                    onClick = {
                        if (inputCode.length == 5) {
                            val success = viewModel.joinLanHostByCode(inputCode, playerNameInput)
                            if (!success) {
                                Toast.makeText(context, "يبدو أن الرمز غير نشط بالشبكة حالياً. تأكد من تشغيل الغرفة من المضيف.", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "الرمز يجب أن يتكون من 5 أرقام", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("ربط", color = GoldShine, fontSize = scaledSp(14))
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(16)))
            Text(
                text = "أو اختر غرفة من كشف الشبكة (UDP):",
                color = DarkWoodButton,
                fontWeight = FontWeight.Bold,
                fontSize = scaledSp(14)
            )
            Spacer(modifier = Modifier.height(scaledDp(6)))
            if (discoveredHosts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(scaledDp(24)))
                        Spacer(modifier = Modifier.height(scaledDp(8)))
                        Text(
                            text = "يبحث عن لغز نشط على الـ WiFi...",
                            color = PapyrusTextSecondary,
                            fontSize = scaledSp(12),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(scaledDp(8))
                ) {
                    discoveredHosts.forEach { (ip, hostDetails) ->
                        val parts = hostDetails.split("|")
                        val hostName = parts.getOrNull(0) ?: "غرفة مجهولة"
                        val rCode = parts.getOrNull(1) ?: "----"
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(scaledDp(10)))
                                    .border(scaledDp(2), GoldYell, RoundedCornerShape(scaledDp(10)))
                                    .clickable {
                                        viewModel.joinLanHost(ip, playerNameInput)
                                    }
                                    .padding(scaledDp(12)),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent, modifier = Modifier.size(scaledDp(24)))
                                Spacer(modifier = Modifier.width(scaledDp(12)))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(14))
                                    Text("رمز الغرفة: $rCode | IP: $ip", color = PapyrusTextSecondary, fontSize = scaledSp(11))
                                }
                                Icon(Icons.Default.ArrowForward, "Join details", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(24)))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Button(
            onClick = {
                LanManager.stopDiscovery()
                onBack()
            },
            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("الرجوع للقائمة الرئيسية", color = GoldShine, fontSize = scaledSp(14))
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
            .padding(scaledDp(24))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ThrillerTitleComponent()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = scaledDp(12)),
            verticalArrangement = Arrangement.spacedBy(scaledDp(14)),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(scaledDp(4), RoundedCornerShape(scaledDp(12)))
                    .clickable { onStartPassPlay() }
                    .testTag("new_game_opt_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(scaledDp(12)),
                border = BorderStroke(scaledDp(2), DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = scaledDp(20), vertical = scaledDp(18))
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.PhoneAndroid, contentDescription = "Pass device", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(36)))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = scaledDp(16)), horizontalAlignment = Alignment.End) {
                        Text(text = "لعبة جديدة", color = Color(0xFF4A1008), fontSize = scaledSp(21), fontWeight = FontWeight.Bold)
                        Text(text = "جرِّب اللعب بالتمرير (جهاز واحد)", color = PapyrusTextSecondary, fontSize = scaledSp(12))
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go play", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(24)))
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(scaledDp(4), RoundedCornerShape(scaledDp(12)))
                    .clickable { onOpenLanJoin() }
                    .testTag("lan_multiplayer_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(scaledDp(12)),
                border = BorderStroke(scaledDp(2), DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = scaledDp(20), vertical = scaledDp(18))
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Wifi, contentDescription = "WiFi game", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(36)))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = scaledDp(16)), horizontalAlignment = Alignment.End) {
                        Text("دخول برمز الغرفة (WiFi)", color = Color(0xFF4A1008), fontSize = scaledSp(21), fontWeight = FontWeight.Bold)
                        Text("العب على جهازك مع أصدقائك بالرمز", color = PapyrusTextSecondary, fontSize = scaledSp(12))
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go LAN Connect", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(24)))
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(scaledDp(4), RoundedCornerShape(scaledDp(12)))
                    .clickable {
                        viewModel.startLanHost("مضيف التحقيق")
                    },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF35120D)),
                shape = RoundedCornerShape(scaledDp(12)),
                border = BorderStroke(scaledDp(2), GoldYell)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = scaledDp(20), vertical = scaledDp(14))
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddBox, "Host Game", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                    Spacer(modifier = Modifier.width(scaledDp(10)))
                    Text(text = "إنشاء ومشاركة غرفة جديدة (Host)", color = GoldShine, fontSize = scaledSp(15), fontWeight = FontWeight.Bold)
                }
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(scaledDp(4), RoundedCornerShape(scaledDp(12)))
                    .clickable { onOpenSettings() }
                    .testTag("settings_button"),
                colors = CardDefaults.cardColors(containerColor = PapyrusBg),
                shape = RoundedCornerShape(scaledDp(12)),
                border = BorderStroke(scaledDp(2), DarkWoodButton)
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = scaledDp(20), vertical = scaledDp(16))
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings Icon", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(30)))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = scaledDp(16)), horizontalAlignment = Alignment.End) {
                        Text("الإعدادات وقواعد اللعب", color = Color(0xFF4A1008), fontSize = scaledSp(18), fontWeight = FontWeight.Bold)
                    }
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go settings", tint = DarkWoodButton, modifier = Modifier.size(scaledDp(24)))
                }
            }
        }
        Text(
            text = " !! القاعدة الاولي والاخيرة ... شك في الجميع",
            color = PapyrusBgLight.copy(alpha = 0.5f),
            fontSize = scaledSp(20),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = scaledDp(20))
        )
    }
}

@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    var revealed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(24))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف الملفات السرية")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        if (!revealed) {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(scaledDp(110))
                        .background(Color(0x3B6E1C11), CircleShape)
                        .padding(scaledDp(16)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = "Hide role cards", tint = GoldShine, modifier = Modifier.fillMaxSize())
                }
                Spacer(modifier = Modifier.height(scaledDp(24)))
                Text(text = "ادي التلفون ل : ", color = PapyrusBgLight.copy(alpha = 0.8f), fontSize = scaledSp(16), textAlign = TextAlign.Center)
                Text(text = activePassPlayer.name, color = GoldShine, fontSize = scaledSp(28), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("pass_name_reveal"))
                Spacer(modifier = Modifier.height(scaledDp(30)))
                Button(onClick = { revealed = true }, colors = ButtonDefaults.buttonColors(containerColor = GoldYell), modifier = Modifier.testTag("reveal_role_button")) {
                    Text("اكتشف الدور السري 👁️", color = Color(0xFF2C150A), fontSize = scaledSp(17), fontWeight = FontWeight.Bold)
                }
            }
        } else {
            ParchmentCard(modifier = Modifier.weight(1f), seed = state.activePassPlayerIndex.toLong()) {
                Text(text = "الملف السري لـ ${activePassPlayer.name}", color = DarkWoodButton, fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(scaledDp(8)))
                Box(
                    modifier = Modifier
                        .size(scaledDp(80))
                        .background(DarkBg, CircleShape)
                        .border(scaledDp(3), GoldYell, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Avatar", tint = GoldShine, modifier = Modifier.size(scaledDp(50)))
                }
                Spacer(modifier = Modifier.height(scaledDp(10)))
                val char = activePassPlayer.character
                if (char != null) {
                    Text("الاسم : ${char.name}", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(20))
                    Text("السن : ${char.age} سنة | المهنة: ${char.occupation}", color = PapyrusTextSecondary, fontSize = scaledSp(15))
                    Text("الصفات : ${char.traits}", color = PapyrusTextSecondary, fontSize = scaledSp(15), fontStyle = FontStyle.Italic)
                    Spacer(modifier = Modifier.height(scaledDp(12)))
                    Divider(color = Color(0x3B2C1E14), thickness = scaledDp(1))
                    Spacer(modifier = Modifier.height(scaledDp(10)))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(scaledDp(8)))
                            .background(if (activePassPlayer.isMafia) RedAccent else InnocentAccent)
                            .padding(scaledDp(12)),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(imageVector = if (activePassPlayer.isMafia) Icons.Default.Dangerous else Icons.Default.Security, contentDescription = "Role Symbol", tint = Color.White, modifier = Modifier.size(scaledDp(24)))
                        Spacer(modifier = Modifier.width(scaledDp(8)))
                        Text(text = if (activePassPlayer.isMafia) "أنت : المجرم الحقيقية" else "أنت : بريء من الجريمة", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(20))
                    }
                    Spacer(modifier = Modifier.height(scaledDp(12)))
                    Text(text = "دافعك المستخبي:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                    Text(text = if (activePassPlayer.isMafia) char.hiddenMotive else "انت برئ حاول تكتشف المجرم الحقيقي !!", color = PapyrusText, fontSize = scaledSp(15), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(16)))
            Button(
                onClick = { viewModel.confirmSecretsRevealed(); revealed = false },
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                shape = RoundedCornerShape(scaledDp(12)),
                modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("confirm_reveal_advance"),
                contentPadding = PaddingValues(scaledDp(14))
            ) {
                Text(text = if (state.activePassPlayerIndex < state.players.size - 1) "خبي ملفك وهات اللي بعده" else "يلا ندخل على تفاصيل القضية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
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
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "تفاصيل الجريمة")
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
            Text(text = currentCase.title, color = Color(0xFF7A1B0C), fontSize = scaledSp(24), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(scaledDp(8))) {
                Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(scaledDp(8))).padding(scaledDp(8)), contentAlignment = Alignment.Center) {
                    Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = scaledSp(11), textAlign = TextAlign.Center)
                }
                Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(scaledDp(8))).padding(scaledDp(8)), contentAlignment = Alignment.Center) {
                    Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = scaledSp(11), textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x12000000), RoundedCornerShape(scaledDp(8))).padding(scaledDp(12))) {
                LazyColumn { item { Text(text = currentCase.description, color = PapyrusText, fontSize = scaledSp(14), lineHeight = scaledSp(21), textAlign = TextAlign.End) } }
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Text(text = " المشتبه فيهم : ", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = scaledSp(13))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(scaledDp(6))) {
                currentCase.characters.forEach { char ->
                    Box(modifier = Modifier.weight(1f).background(Color(0xFF8C2012), RoundedCornerShape(scaledDp(6))).padding(scaledDp(6)), contentAlignment = Alignment.Center) {
                        Text(text = char.name.split(" ").firstOrNull() ?: char.name, color = Color.White, fontSize = scaledSp(11), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Button(onClick = { viewModel.startCaseInvestigationIntro() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), modifier = Modifier.fillMaxWidth().testTag("case_details_confirm_button"), contentPadding = PaddingValues(scaledDp(15))) {
            Icon(Icons.Default.FindInPage, "Start Clues", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
            Spacer(modifier = Modifier.width(scaledDp(8)))
            Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(16))
        }
    }
}

@Composable
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    val clueIndex = state.currentEvidenceIndex
    val currentClue = currentCase.evidenceList.getOrNull(clueIndex) ?: "لا أدلة إضافية حالياً."
    var showHint by remember(clueIndex) { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "الدليل الجنائي ${clueIndex + 1} من ${currentCase.evidenceList.size}")
        Spacer(modifier = Modifier.height(scaledDp(12)))
        ParchmentCard(modifier = Modifier.weight(1f), seed = (clueIndex + 10).toLong()) {
            Box(modifier = Modifier.size(scaledDp(90)).background(Color(0xFF35120D), CircleShape).border(scaledDp(2), GoldShine, CircleShape).shadow(scaledDp(4), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Search, contentDescription = "Evidence Seal", tint = GoldShine, modifier = Modifier.size(scaledDp(48)))
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Text(text = "دليل جديد تم استنتاجه مفاجئ:", color = Color(0xFF531E17), fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0F000000), RoundedCornerShape(scaledDp(10))).padding(scaledDp(14)), contentAlignment = Alignment.Center) {
                Text(text = currentClue, color = PapyrusText, fontSize = scaledSp(15), lineHeight = scaledSp(22), textAlign = TextAlign.Center)
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            AnimatedVisibility(visible = showHint) {
                Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(scaledDp(8))).background(Color(0xFFFFF2CD)).border(scaledDp(1), Color(0xFFFFCD56), RoundedCornerShape(scaledDp(8))).padding(scaledDp(10))) {
                    Text(text = currentCase.hint, color = Color(0xFF856404), fontSize = scaledSp(12), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                }
            }
            if (!showHint) {
                Button(onClick = { showHint = true }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE2A012)), modifier = Modifier.testTag("clue_hint_button")) {
                    Icon(Icons.Default.Warning, "Clues Alert", tint = Color.Black, modifier = Modifier.size(scaledDp(20)))
                    Spacer(modifier = Modifier.width(scaledDp(6)))
                    Text("عرض تلميح  💡", color = Color.Black, fontSize = scaledSp(12), fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Button(onClick = { viewModel.advanceFromEvidenceToDiscussion() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), modifier = Modifier.fillMaxWidth().testTag("evidence_reveal_advance"), contentPadding = PaddingValues(scaledDp(15))) {
            Icon(Icons.Default.RecordVoiceOver, "Discuss", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
            Spacer(modifier = Modifier.width(scaledDp(8)))
            Text("فتح طاولة النقاش والمواجهة 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
        }
    }
}

@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    var suspectedByClick = remember { mutableStateListOf<String>() }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(20))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "مرحلة النقاش والمواجهة")
        Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val formattedTime = String.format("%02d:%02d", state.timerSecondsLeft / 60, state.timerSecondsLeft % 60)
            Canvas(modifier = Modifier.size(scaledDp(170))) {
                drawCircle(color = Color(0xFF1E0604), radius = size.minDimension / 2)
                val sweepAngle = if (state.timerTotalSeconds > 0) (state.timerSecondsLeft.toFloat() / state.timerTotalSeconds.toFloat()) * 360f else 360f
                drawArc(color = Color(0xFFE73224), startAngle = -90f, sweepAngle = sweepAngle, useCenter = false, style = Stroke(width = scaledDp(8).value, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("متبقي", color = GoldYell, fontSize = scaledSp(12))
                Text(text = formattedTime, color = Color.White, fontSize = scaledSp(28), fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace, modifier = Modifier.testTag("timer_countdown_display"))
                Text("للإدلاء بالاستنتاج", color = PapyrusBgLight.copy(alpha = 0.5f), fontSize = scaledSp(10))
            }
            val alivePlayers = state.players.filter { it.isAlive }
            alivePlayers.forEachIndexed { index, player ->
                val angleRad = (2 * Math.PI * index) / alivePlayers.size
                // حساب الإزاحة بحيث تكون Dp
                val xOffset = (scaledDp(130).value * cos(angleRad)).toFloat().dp
                val yOffset = (scaledDp(130).value * sin(angleRad)).toFloat().dp
                val isClickSuspected = player.id in suspectedByClick
                Box(
                    modifier = Modifier
                        .offset(x = xOffset, y = yOffset)
                        .size(scaledDp(68))
                        .shadow(scaledDp(3), CircleShape)
                        .background(if (isClickSuspected) Color(0xFFC42512) else Color(0xFF421E14), CircleShape)
                        .border(scaledDp(2), if (isClickSuspected) GoldShine else Color(0x3BFFFFFF), CircleShape)
                        .clickable {
                            if (isClickSuspected) suspectedByClick.remove(player.id) else suspectedByClick.add(player.id)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.padding(scaledDp(4))) {
                        Text(text = player.name.take(6), color = Color.White, fontSize = scaledSp(10), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(modifier = Modifier.height(scaledDp(2)))
                        Box(modifier = Modifier.background(if (player.isMafia && isClickSuspected) GoldYell else Color(0x3B000000), RoundedCornerShape(scaledDp(4))).padding(horizontal = scaledDp(4), vertical = scaledDp(2))) {
                            Text(text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال", color = if (isClickSuspected) Color.Black else Color.White, fontSize = scaledSp(8), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(10)))
        ParchmentCard(modifier = Modifier.wrapContentHeight(), seed = 771L) {
            Text(text = "دوس على أي لاعب عشان تركز الشكوك عليه باللون الأحمر عشان تبدأوا تناقشوه.", color = PapyrusTextSecondary, fontSize = scaledSp(15), textAlign = TextAlign.Center)
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Button(onClick = { viewModel.advanceFromDiscussionToVoting() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("voting_advance_button")) {
            Icon(Icons.Default.HowToVote, "Start Votes", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
            Spacer(modifier = Modifier.width(scaledDp(8)))
            Text("يلا ندخل على الاقتراع والتصويت", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(20))
        }
    }
}

@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    if (state.mode == "PASS_AND_PLAY") {
        val voterPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
        var isDevicePassed by remember(state.activePassPlayerIndex) { mutableStateOf(false) }
        if (!isDevicePassed) {
            MysteryBackground {
                Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ParchmentHeaderBanner(text = "صندوق التصويت")
                    Spacer(modifier = Modifier.height(scaledDp(30)))
                    Text(text = "هات الموبايل ووريه لـ/ ${voterPlayer.name}", color = PapyrusBgLight, fontSize = scaledSp(26), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(12)))
                    Text(text = "فكر قبل ما تصوت .....شغل دماغك !!!", color = Color.LightGray, fontSize = scaledSp(17), textAlign = TextAlign.Center, modifier = Modifier.padding(scaledDp(16)))
                    Spacer(modifier = Modifier.height(scaledDp(30)))
                    Button(onClick = { isDevicePassed = true }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56))) {
                        Text("يلا نصوّت", color = Color.White, fontSize = scaledSp(20), fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }
        var selectedTargetId by remember { mutableStateOf("") }
        val eligibleCandidates = if (state.tiedVotePlayers.isNotEmpty()) {
            state.players.filter { it.id in state.tiedVotePlayers && it.id != voterPlayer.id }
        } else {
            state.players.filter { it.isAlive && it.id != voterPlayer.id }
        }
        Column(modifier = Modifier.fillMaxSize().padding(scaledDp(20)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
            Spacer(modifier = Modifier.height(scaledDp(10)))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 33L) {
                Text(text = "دور اللاعب: ${voterPlayer.name}", color = Color(0xFF6E1B10), fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "اختار الشخص اللي شاكك فيه تفتكر هو المجرم:", color = PapyrusTextSecondary, fontSize = scaledSp(15), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(scaledDp(12)))
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(scaledDp(8))) {
                    items(eligibleCandidates) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(scaledDp(10))).border(scaledDp(2), if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(scaledDp(10))).clickable { selectedTargetId = candidate.id }.padding(scaledDp(14)), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(scaledDp(38)).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(scaledDp(20)))
                            }
                            Spacer(modifier = Modifier.width(scaledDp(14)))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
                                candidate.character?.let { Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = scaledSp(14)) }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(16)))
            Button(onClick = {
                if (selectedTargetId.isBlank()) Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                else { viewModel.submitVote(selectedTargetId); selectedTargetId = "" }
            }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("submit_vote_action_button")) {
                Text("أكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(18))
            }
        }
    } else {
        val localVoter = state.players.find { it.id == viewModel.myPlayerId.value } ?: return
        if (!localVoter.isAlive) {
            MysteryBackground {
                Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ParchmentHeaderBanner(text = " أنت برة اللعب دلوقتي 💀")
                    Spacer(modifier = Modifier.height(scaledDp(24)))
                    Text(text = "مبروك تصفيتك! استنى تصويت باقي اللعيبة...", color = PapyrusBgLight, fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        } else if (state.votes.containsKey(localVoter.id)) {
            val activePlayers = state.players.filter { it.isAlive }
            val waitingPlayers = activePlayers.filter { it.id !in state.votes.keys }
            MysteryBackground {
                Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ParchmentHeaderBanner(text = "تم تسجيل صوتك بنجاح! 🗳️")
                    Spacer(modifier = Modifier.height(scaledDp(24)))
                    Text(text = "مستنيين باقي اللعيبة يصوتوا...", color = PapyrusBgLight, fontSize = scaledSp(25), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(20)))
                    ParchmentCard(modifier = Modifier.fillMaxWidth().heightIn(max = scaledDp(350))) {
                        Column(modifier = Modifier.padding(scaledDp(16))) {
                            Text("الشفافية والتصويت المفتوح المباشر:", color = Color(0xFF6E1B10), fontSize = scaledSp(18), fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(scaledDp(8)))
                            val votesCast = state.votes.mapNotNull { (vId, tId) ->
                                val voterName = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
                                val targetName = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
                                "👈 اللاعب $voterName صوّت ضد $targetName"
                            }
                            if (votesCast.isEmpty()) Text("في انتظار الصوت العلني الأول لبدء كشف التواطؤ... 🗳️", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                            else votesCast.forEach { voteLine -> Text(text = voteLine, color = PapyrusText, fontSize = scaledSp(15), fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = scaledDp(4))) }
                            Spacer(modifier = Modifier.height(scaledDp(16)))
                            Text("مين اللي لسه مصوّتش:", color = Color(0xFF6E1B10), fontSize = scaledSp(16), fontWeight = FontWeight.Bold)
                            val waitingNames = waitingPlayers.joinToString { it.name }.ifEmpty { "الجميع أدلى بصوته علناً!" }
                            Text(waitingNames, color = PapyrusTextSecondary, fontSize = scaledSp(14))
                        }
                    }
                }
            }
        } else {
            var selectedTargetId by remember { mutableStateOf("") }
            val eligibleCandidates = if (state.tiedVotePlayers.isNotEmpty()) {
                state.players.filter { it.id in state.tiedVotePlayers && it.id != localVoter.id }
            } else {
                state.players.filter { it.isAlive && it.id != localVoter.id }
            }
            Column(modifier = Modifier.fillMaxSize().padding(scaledDp(20)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
                Spacer(modifier = Modifier.height(scaledDp(10)))
                ParchmentCard(modifier = Modifier.weight(1f), seed = 33L) {
                    Text(text = "دورك في التصويت: ${localVoter.name}", color = Color(0xFF6E1B10), fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(text = "اختار الشخص اللي شاكك فيه تفتكر هو المجرم:", color = PapyrusTextSecondary, fontSize = scaledSp(15), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(12)))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(scaledDp(8))) {
                        items(eligibleCandidates) { candidate ->
                            val isSelected = candidate.id == selectedTargetId
                            Row(modifier = Modifier.fillMaxWidth().background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(scaledDp(10))).border(scaledDp(2), if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(scaledDp(10))).clickable { selectedTargetId = candidate.id }.padding(scaledDp(14)), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(scaledDp(38)).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(scaledDp(20)))
                                }
                                Spacer(modifier = Modifier.width(scaledDp(14)))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
                                    candidate.character?.let { Text("المشتبه: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = scaledSp(14)) }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(scaledDp(16)))
                Button(onClick = {
                    if (selectedTargetId.isBlank()) Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                    else viewModel.submitVote(selectedTargetId)
                }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("submit_vote_action_button")) {
                    Text("أكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(18))
                }
            }
        }
    }
}

@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val eliminatedPlayers = state.players.filter { !it.isAlive }
    val remainingSuspects = state.players.filter { it.isAlive }
    val localPlayer = state.players.find { it.id == viewModel.myPlayerId.value }
    if (state.mode == "PASS_AND_PLAY") {
        val juryVoter = eliminatedPlayers.firstOrNull { it.id !in state.juryVotes.keys }
        var isDevicePassed by remember(juryVoter?.id) { mutableStateOf(false) }
        if (juryVoter != null && !isDevicePassed) {
            MysteryBackground {
                Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    ParchmentHeaderBanner(text = "مرر الموبايل")
                    Spacer(modifier = Modifier.height(scaledDp(30)))
                    Text(text = "هات الموبايل ووريه ل /  ${juryVoter.name}", color = PapyrusBgLight, fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(8)))
                    Text(text = "تصويتك هيكون مهم ومصير الباقيين في إيدك .... متبقاش غبي !!", color = Color.LightGray, fontSize = scaledSp(15), textAlign = TextAlign.Center, modifier = Modifier.padding(scaledDp(16)))
                    Spacer(modifier = Modifier.height(scaledDp(30)))
                    Button(onClick = { isDevicePassed = true }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56))) {
                        Text("ادخل صوّت ", color = Color.White, fontSize = scaledSp(18), fontWeight = FontWeight.Bold)
                    }
                }
            }
            return
        }
        Column(modifier = Modifier.fillMaxSize().padding(scaledDp(20)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
            ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
            Spacer(modifier = Modifier.height(scaledDp(10)))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                Box(modifier = Modifier.size(scaledDp(80)).background(Color(0x3B6E1B10), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Gavel, contentDescription = "Gavel judge", tint = RedAccent, modifier = Modifier.size(scaledDp(48)))
                }
                Spacer(modifier = Modifier.height(scaledDp(8)))
                Text(text = "!!! لا تقلقوا ولكن احذروا !!!", color = Color(0xFF6E1D10), fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(22), textAlign = TextAlign.Center)
                Text(text = "بما أنه لم يتبق سوى لاعبين اثنين، يعود اللاعبين الذين تم تصفيتهم سابقاً للإجماع والتصويت لإثبات الإدانة النهائية على المجرم.", color = PapyrusTextSecondary, fontSize = scaledSp(14), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(scaledDp(10)))
                if (juryVoter != null) {
                    Text(text = "دور اللاعب : ${juryVoter.name}", color = RedAccent, fontWeight = FontWeight.Bold, fontSize = scaledSp(18), modifier = Modifier.testTag("jury_voter_title"))
                    Spacer(modifier = Modifier.height(scaledDp(8)))
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(scaledDp(8))) {
                        items(remainingSuspects) { suspect ->
                            Row(modifier = Modifier.fillMaxWidth().background(Color(0x0C000000), RoundedCornerShape(scaledDp(10))).border(scaledDp(1), Color(0x3B2C1E14), RoundedCornerShape(scaledDp(10))).padding(scaledDp(12)).clickable { viewModel.submitJuryVote(suspect.id) }, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
                                    suspect.character?.let { Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = scaledSp(14)) }
                                }
                                Button(onClick = { viewModel.submitJuryVote(suspect.id) }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent)) {
                                    Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                                }
                            }
                        }
                    }
                } else {
                    Text(text = "تم جمع كافة استنتاجات اللاعبين بنجاح. سنعلن النتيجة الآن!", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = scaledSp(18), textAlign = TextAlign.Center)
                }
            }
        }
    } else {
        if (localPlayer == null) return
        val isAlive = localPlayer.isAlive
        val hasVoted = state.juryVotes.containsKey(localPlayer.id)
        MysteryBackground {
            Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                if (isAlive) {
                    ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                    Spacer(modifier = Modifier.height(scaledDp(24)))
                    Text(text = "هيئة المحلفين بتصوّت دلوقتي...", color = PapyrusBgLight, fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(12)))
                    Text(text = "مصيرك وصاحبك الأخير بين إيدين  اللاعبين اللي خرجوا ! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟", color = Color.LightGray, fontSize = scaledSp(16), textAlign = TextAlign.Center)
                } else {
                    if (hasVoted) {
                        ParchmentHeaderBanner(text = "تم تسجيل صوتك للمحلفين! ⚖️")
                        Spacer(modifier = Modifier.height(scaledDp(24)))
                        Text(text = "مستنيين باقي اللاعبين عشان تظهر النتيجة...", color = PapyrusBgLight, fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    } else {
                        ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                        Spacer(modifier = Modifier.height(scaledDp(16)))
                        ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                            Text(text = "اضغط إدانة على المجرم الحقيقي عشان تحسم الجريمة وترجع حق الضحية!", color = Color(0xFF6E1D10), fontWeight = FontWeight.Bold, fontSize = scaledSp(18), textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(scaledDp(16)))
                            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(scaledDp(8))) {
                                items(remainingSuspects) { suspect ->
                                    Row(modifier = Modifier.fillMaxWidth().background(Color(0x0C000000), RoundedCornerShape(scaledDp(10))).border(scaledDp(1), Color(0x3B2C1E14), RoundedCornerShape(scaledDp(10))).padding(scaledDp(12)), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
                                            suspect.character?.let { Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = scaledSp(14)) }
                                        }
                                        Button(onClick = { viewModel.submitJuryVote(suspect.id) }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent)) {
                                            Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase
    val isInnocentsWinner = state.winnerSide == "INNOCENTS"
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaledDp(24))
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        ParchmentHeaderBanner(text = "كشف أوراق القضية النهائية")
        Spacer(modifier = Modifier.height(scaledDp(14)))
        ParchmentCard(modifier = Modifier.weight(1f), seed = 4441L) {
            Box(modifier = Modifier.size(scaledDp(100)).background(Color(0x1FA2A012), CircleShape).border(scaledDp(2), GoldYell, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.EmojiEvents, contentDescription = "Trophy logo endgame", tint = GoldYell, modifier = Modifier.size(scaledDp(64)))
            }
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Text(text = if (isInnocentsWinner) "!!الف مبرووك عرفتوا تطلعوا المجرم الفاشل!!" else "!المجرم انتصر وضحك على الكل!", color = if (isInnocentsWinner) GreenAccent else RedAccent, fontSize = scaledSp(26), fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, modifier = Modifier.testTag("endgame_victory_title"))
            Spacer(modifier = Modifier.height(scaledDp(10)))
            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0x0C000000), RoundedCornerShape(scaledDp(10))).padding(scaledDp(14))) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    item {
                        Text(text = if (isInnocentsWinner) "الأبرياء عرفوا يجمعوا الأدلة ويكشفوا اللعبة الصح، والمجرم وقع في شر أعماله ." else "المجرم عرف يضحك على الكل ويثبت تهم باطلة على الأبرياء، وخرج من القضية زي الشعرة من العجين.", color = PapyrusText, fontSize = scaledSp(16), lineHeight = scaledSp(24), textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(scaledDp(14)))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(scaledDp(10)))
                        Text(text = "الهوية الحقيقية للمجرم:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = scaledSp(18), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().testTag("dramatic_criminal_reveal_header"))
                        state.players.filter { it.isMafia }.forEach { mafia ->
                            Spacer(modifier = Modifier.height(scaledDp(8)))
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)), border = BorderStroke(scaledDp(1), Color(0xFFE63946)), shape = RoundedCornerShape(scaledDp(8))) {
                                Column(modifier = Modifier.padding(scaledDp(12))) {
                                    Text(text = "المجرم الحقيقي: ${mafia.name}", color = Color(0xFFD62828), fontWeight = FontWeight.ExtraBold, fontSize = scaledSp(18), modifier = Modifier.testTag("criminal_character_name"))
                                    Spacer(modifier = Modifier.height(scaledDp(4)))
                                    Text("العمر: ${mafia.character?.age ?: 30} سنة | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = scaledSp(15))
                                    Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                                    Text("المستوى الاجتماعي: ${mafia.character?.socialStatus ?: "متوسط الحال"}", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                                    Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                                    Text("علاقته بالمشتبهين: ${mafia.character?.relationshipToOtherSuspects ?: "منافسة"}", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                                    Text("السجل الجنائي: ${mafia.character?.relevantHistory ?: "خالي من السوابق"}", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                                    Spacer(modifier = Modifier.height(scaledDp(6)))
                                    Text(text = "الدافع والنية المستخبية: ${mafia.character?.hiddenMotive ?: ""}", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = scaledSp(15), lineHeight = scaledSp(20))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(scaledDp(14)))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(scaledDp(14)))
                        Text(text = "المخطط الكامل وسيناريو الجريمة الداخلي:", color = Color(0xFF355E3B), fontWeight = FontWeight.Bold, fontSize = scaledSp(17), modifier = Modifier.fillMaxWidth().testTag("case_explanation_header"))
                        Spacer(modifier = Modifier.height(scaledDp(8)))
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1F2A9D8F)), shape = RoundedCornerShape(scaledDp(8))) {
                            Text(text = currentCase?.explanation ?: "لم تتوفر سجلات سردية للملف.", color = Color(0xFF1D3557), fontSize = scaledSp(15), lineHeight = scaledSp(22), modifier = Modifier.padding(scaledDp(12)).testTag("case_explanation_text"))
                        }
                        Spacer(modifier = Modifier.height(scaledDp(16)))
                        HorizontalDivider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(scaledDp(10)))
                        Text(text = "كشف هويات كل اللاعبين بغرفة التحقيق:", color = DarkWoodButton, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                        Spacer(modifier = Modifier.height(scaledDp(8)))
                        state.players.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = scaledDp(4)), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (p.isMafia) "مجرم" else "بريء ", color = if (p.isMafia) RedAccent else InnocentAccent, fontWeight = FontWeight.Bold, fontSize = scaledSp(15))
                                Text(text = "${p.name} (${p.character?.name ?: ""})", color = PapyrusTextSecondary, fontSize = scaledSp(15))
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(scaledDp(16)))
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(scaledDp(8))) {
            Button(onClick = { viewModel.playButtonClick(); viewModel.playAgain() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("play_again_button"), contentPadding = PaddingValues(scaledDp(15))) {
                Icon(Icons.Default.Refresh, "Play again", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                Spacer(modifier = Modifier.width(scaledDp(8)))
                Text("لعب جولة وقضية جديدة", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
            }
            OutlinedButton(onClick = { viewModel.playButtonClick(); viewModel.resetToMainMenu() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = GoldShine), shape = RoundedCornerShape(scaledDp(12)), modifier = Modifier.fillMaxWidth().height(scaledDp(56))) {
                Icon(Icons.Default.Home, "Main menu", tint = GoldShine, modifier = Modifier.size(scaledDp(24)))
                Spacer(modifier = Modifier.width(scaledDp(8)))
                Text("العودة للقائمة الرئيسية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
            }
        }
    }
}

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
            Button(onClick = { viewModel.updateSettings(discTimeMins, voteTimeMins, soundEnabled, sliderVol); onDismissRequest() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton), shape = RoundedCornerShape(scaledDp(12))) {
                Text("حفظ التعديلات", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text("إلغاء", color = PapyrusTextSecondary, fontSize = scaledSp(16)) }
        },
        title = {
            Text(text = "إعدادات وقواعد اللعبة", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = scaledSp(22), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            ParchmentCard(seed = 77L, contentPadding = PaddingValues(scaledDp(12)), modifier = Modifier.wrapContentHeight()) {
                LazyColumn(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(scaledDp(10))) {
                    item {
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = RedAccent))
                            Text("المؤثرات الصوتية والموسيقى", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                        }
                        Spacer(modifier = Modifier.height(scaledDp(4)))
                        Text("درجة الصوت: ${(sliderVol * 100).toInt()}%", color = PapyrusTextSecondary, fontSize = scaledSp(14))
                        Slider(value = sliderVol, onValueChange = { sliderVol = it }, modifier = Modifier.fillMaxWidth())
                        Divider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(scaledDp(6)))
                        Text("وقت جولات المناقشة والتحقيق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { if (discTimeMins > 1) discTimeMins-- }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("-", fontSize = scaledSp(18), fontWeight = FontWeight.Bold) }
                            Text("$discTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(16), modifier = Modifier.align(Alignment.CenterVertically))
                            Button(onClick = { if (discTimeMins < 10) discTimeMins++ }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("+", fontSize = scaledSp(18), fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(scaledDp(6)))
                        Text("وقت جولات الاقتراع والتصويت", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(16))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            Button(onClick = { if (voteTimeMins > 1) voteTimeMins-- }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("-", fontSize = scaledSp(18), fontWeight = FontWeight.Bold) }
                            Text("$voteTimeMins دقائق", color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = scaledSp(16), modifier = Modifier.align(Alignment.CenterVertically))
                            Button(onClick = { if (voteTimeMins < 5) voteTimeMins++ }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) { Text("+", fontSize = scaledSp(18), fontWeight = FontWeight.Bold) }
                        }
                        Spacer(modifier = Modifier.height(scaledDp(10)))
                        Divider(color = Color(0x3B2C1E14))
                        Spacer(modifier = Modifier.height(scaledDp(6)))
                        Text("قوانين اللعبة الأساسية:", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = scaledSp(18))
                        Text(text = "1. اللعبة تدعم من 4 لـ 6 لاعبين.\n" + "2. لو عدد اللاعبين 4، بيكون فيه مجرم واحدة بس؛ ولو أكتر من كدة بيتم تعيين 2 مجرم تلقائياً لدعم التحدي والمنافسة.\n" + "3. في نهاية الجولة لو اتبقى اتنين مشتبه بيهم بس عايشين، بيتلغي تصويت الاقتراع المباشر واللعيبة اللي خرجوا بترجع تلقائياً كـ (هيئة المحلفين) لحسم القرار النهائي وإدانة المجرم الحقيقية.", color = PapyrusTextSecondary, fontSize = scaledSp(15), lineHeight = scaledSp(22))
                    }
                }
            }
        },
        containerColor = PapyrusBg
    )
}

@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.mode == "PASS_AND_PLAY" || state.hostId == viewModel.myPlayerId.value
    MysteryBackground {
        Column(modifier = Modifier.fillMaxSize().padding(scaledDp(24)).safeDrawingPadding().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            ParchmentHeaderBanner(text = "نتائج الاقتراع العام")
            Spacer(modifier = Modifier.height(scaledDp(24)))
            ParchmentCard(modifier = Modifier.fillMaxWidth().padding(scaledDp(8))) {
                Column(modifier = Modifier.fillMaxWidth().padding(scaledDp(20)), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(imageVector = if (state.tiedVotePlayers.isNotEmpty()) Icons.Default.Warning else Icons.Default.Info, contentDescription = "Result Icon", tint = if (state.tiedVotePlayers.isNotEmpty()) Color(0xFFC62828) else GoldShine, modifier = Modifier.size(scaledDp(64)))
                    Spacer(modifier = Modifier.height(scaledDp(16)))
                    Text(text = state.lastEliminatedResult, color = Color(0xFF1C130C), fontSize = scaledSp(24), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = scaledSp(32), modifier = Modifier.testTag("vote_result_text"))
                    if (state.tiedVotePlayers.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(scaledDp(16)))
                        Text(text = "قانون تصفية التعادل: سيتم تكرار جولة التصويت الآن لتكون محصورة ومقتصرة فقط على المشتبهين المتساوين بالأصوات حتى التوصل إلى أغلبية حاسمة تفصل الشك بالحقيقة.", color = Color(0xFFB71C1C), fontSize = scaledSp(16), textAlign = TextAlign.Center, lineHeight = scaledSp(22))
                    }
                    Spacer(modifier = Modifier.height(scaledDp(24)))
                    Text(text = "كشف الأصوات العامة  : 🗳️", color = Color(0xFF6E1B10), fontSize = scaledSp(17), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(scaledDp(8)))
                    val votesSummary = state.votes.mapNotNull { (vId, tId) ->
                        val voter = state.players.find { it.id == vId }?.name ?: return@mapNotNull null
                        val target = state.players.find { it.id == tId }?.name ?: return@mapNotNull null
                        "👤 $voter ➔ صوّت ضد 🎯 $target"
                    }
                    if (votesSummary.isEmpty()) Text("لم يتم الإدلاء بأي أصوات.", color = Color.Gray, fontSize = scaledSp(14))
                    else votesSummary.forEach { voteText -> Text(text = voteText, color = Color(0xFF2C1E14), fontSize = scaledSp(16), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = scaledDp(2))) }
                }
            }
            Spacer(modifier = Modifier.height(scaledDp(32)))
            if (isHost) {
                Button(onClick = { viewModel.playButtonClick(); viewModel.confirmVoteResultAndProceed() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), modifier = Modifier.fillMaxWidth().height(scaledDp(56)).testTag("confirm_vote_result_button"), shape = RoundedCornerShape(scaledDp(12))) {
                    Text(text = if (state.tiedVotePlayers.isNotEmpty()) "بدء جولة حسم التعادل" else "متابعة مسار التحقيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = scaledSp(20))
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)), shape = RoundedCornerShape(scaledDp(12))) {
                    Text(text = "في انتظار المضيف لمتابعة القضية...", color = PapyrusBgLight, fontSize = scaledSp(18), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(scaledDp(16)))
                }
            }
        }
    }
}