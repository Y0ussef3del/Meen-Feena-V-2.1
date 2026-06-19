package com.example.ui.screens

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.model.RoomState
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.model.*
import com.example.game.network.LanManager
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import kotlin.math.min

// ==========================================
// Helper functions for responsive design
// ==========================================
@Composable
fun responsiveTitleSize(maxWidth: Dp): androidx.compose.ui.unit.TextUnit {
    return when {
        maxWidth < 360.dp -> 32.sp
        maxWidth < 600.dp -> 48.sp
        else -> 65.sp
    }
}

@Composable
fun responsivePadding(maxWidth: Dp): Dp {
    return when {
        maxWidth < 360.dp -> 12.dp
        maxWidth < 600.dp -> 20.dp
        else -> 32.dp
    }
}

@Composable
fun isLandscape(maxWidth: Dp, maxHeight: Dp): Boolean {
    return maxWidth > maxHeight
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val context = LocalContext.current
    val activity = LocalActivity.current

    // Keep screen on and immersive mode
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    DisposableEffect(state.phase) {
        activity?.let { act ->
            WindowCompat.setDecorFitsSystemWindows(act.window, false)
            val controller = WindowCompat.getInsetsController(act.window, act.window.decorView)
            controller?.let {
                it.hide(WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars())
                it.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose { /* no-op */ }
    }

    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2200)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MysteryBackground(drawBloodDrips = state.phase == GamePhase.LOBBY) {
            AnimatedContent(
                targetState = state.phase,
                transitionSpec = {
                    val duration = 800
                    (fadeIn(animationSpec = tween(duration)) +
                            slideInVertically(initialOffsetY = { 80 }, animationSpec = tween(duration)) +
                            scaleIn(initialScale = 0.95f, animationSpec = tween(duration))) togetherWith
                            (fadeOut(animationSpec = tween(500)) +
                                    scaleOut(targetScale = 1.05f, animationSpec = tween(500)))
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

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = tween(600))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
                SplashScreen()
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun ThrillerTitleComponent(
    fontSize: androidx.compose.ui.unit.TextUnit = 65.sp,
    maxWidth: Dp? = null
) {
    val adjustedSize = if (maxWidth != null) responsiveTitleSize(maxWidth) else fontSize
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "مين فينا ؟",
            color = GoldYell,
            fontSize = adjustedSize,
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ThrillerTitleComponent(fontSize = responsiveTitleSize(this.maxWidth) * 0.83f, maxWidth = this.maxWidth)
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                color = RedAccent,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.weight(0.5f))
            Text(
                text = "الكل متهم .......ولكن ؟",
                color = PapyrusBgLight.copy(alpha = 0.5f),
                fontSize = if (this.maxWidth < 360.dp) 18.sp else 30.sp,
                fontStyle = FontStyle.Italic,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

// ==========================================
// 2. MAIN MENU & LOBBY SYSTEM
// ==========================================
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "اوضة المضيف")
            Spacer(modifier = Modifier.height(10.dp))
            ThrillerTitleComponent(fontSize = responsiveTitleSize(this.maxWidth) * 0.58f, maxWidth = this.maxWidth)
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 789L) {
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
                    val codeFontSize = if (34.sp.value < (this.maxWidth.value * 0.06f)) 34.sp else (this.maxWidth.value * 0.06f).sp
                    Text(
                        text = state.roomId,
                        color = GoldShine,
                        fontSize = codeFontSize,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 6.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "اللاعبين المنضمون (${state.players.size}) : ",
                    color = Color(0xFF4A1008),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.players, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(DarkWoodButton, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = player.name + if (player.id == state.hostId) " (مضيف)" else "",
                                color = PapyrusText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        text = "الاوضة جاهزة لبدء القضية!",
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
                    Text("إلغاء الاوضة", color = GoldShine)
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
}

@Composable
fun ClientWaitingScreen(viewModel: GameViewModel, state: RoomState) {
    val myName = viewModel.myPlayerName.collectAsState().value
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "في انتظار التحقيق")
            Spacer(modifier = Modifier.height(10.dp))
            ThrillerTitleComponent(fontSize = responsiveTitleSize(this.maxWidth) * 0.58f, maxWidth = this.maxWidth)
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 999L) {
                Text(
                    text = "أنت منضم للاوضة رقم:",
                    color = PapyrusTextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                val codeFontSize = if (30.sp.value < (this.maxWidth.value * 0.08f)) 30.sp else (this.maxWidth.value * 0.08f).sp
                Text(
                    text = state.roomId,
                    color = Color(0xFF4A1008),
                    fontSize = codeFontSize,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
                Spacer(modifier = Modifier.height(12.dp))
                Box(modifier = Modifier.size(60.dp), contentAlignment = Alignment.Center) {
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
                    items(state.players, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x06000000), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x142C1E14), RoundedCornerShape(8.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(28.dp)
                                    .background(if (player.name == myName) RedAccent else DarkWoodButton, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = player.name + if (player.name == myName) " (أنت)" else "",
                                color = PapyrusText,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
}

@Composable
fun LocalSetupScreen(viewModel: GameViewModel, state: RoomState, onBack: () -> Unit) {
    val context = LocalContext.current
    var tempPlayerName by remember { mutableStateOf("") }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val isSmall = this.maxWidth < 360.dp
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "إعداد اللاعبين")
            Spacer(modifier = Modifier.height(10.dp))
            ThrillerTitleComponent(fontSize = responsiveTitleSize(this.maxWidth) * 0.5f, maxWidth = this.maxWidth)
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 123L) {
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
                if (isSmall) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            modifier = Modifier.fillMaxWidth().testTag("player_name_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (tempPlayerName.isNotBlank()) {
                                    if (state.players.size < 6) {
                                        viewModel.addLocalLobbyPlayer(tempPlayerName)
                                        tempPlayerName = ""
                                    } else {
                                        Toast.makeText(context, "اخرك 6 لاعيبة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                            modifier = Modifier.fillMaxWidth().testTag("add_player_button")
                        ) {
                            Icon(Icons.Default.Add, "Add player", tint = GoldShine)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة", color = GoldShine)
                        }
                    }
                } else {
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
                            modifier = Modifier.weight(1f).testTag("player_name_input"),
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                if (tempPlayerName.isNotBlank()) {
                                    if (state.players.size < 6) {
                                        viewModel.addLocalLobbyPlayer(tempPlayerName)
                                        tempPlayerName = ""
                                    } else {
                                        Toast.makeText(context, "اخرك 6 لاعيبة", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                            modifier = Modifier.testTag("add_player_button")
                        ) {
                            Icon(Icons.Default.Add, "Add player", tint = GoldShine)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.players, key = { it.id }) { player ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x0C000000), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x1F2C1E14), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier.size(36.dp).background(DarkWoodButton, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = player.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = player.name,
                                color = PapyrusText,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                        if (state.players.size < 4) Toast.makeText(context, "اقل حاجة 4 لاعيبة", Toast.LENGTH_SHORT).show()
                        else viewModel.startInvestigationGame()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    modifier = Modifier.weight(1.5f).testTag("start_game_button"),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, "Start", tint = GoldShine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ابدأ اللعبة 🔍", color = GoldShine, fontWeight = FontWeight.Bold)
                }
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
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParchmentHeaderBanner(text = "الانضمام للاوضة")
            Spacer(modifier = Modifier.height(10.dp))
            ThrillerTitleComponent(fontSize = responsiveTitleSize(this.maxWidth) * 0.5f, maxWidth = this.maxWidth)
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 456L) {
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
                        onValueChange = { if (it.length <= 5) inputCode = it.filter { char -> char.isDigit() } },
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
                                if (!success) Toast.makeText(context, "يبدو أن الرمز غير نشط بالشبكة حالياً. تأكد من تشغيل الاوضة من المضيف.", Toast.LENGTH_LONG).show()
                            } else Toast.makeText(context, "الرمز يجب أن يتكون من 5 أرقام", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("ربط", color = GoldShine)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "أأو اختر اوضة من كشف الشبكة (UDP):",
                    color = DarkWoodButton,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (discoveredHosts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = DarkWoodButton, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("يبحث عن لغز نشط على الـ WiFi...", color = PapyrusTextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    val hostsList = remember(discoveredHosts) { discoveredHosts.toList() }
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(hostsList, key = { it.first }) { (ip, hostDetails) ->
                            val parts = remember(hostDetails) { hostDetails.split("|") }
                            val hostName = parts.getOrNull(0) ?: "اوضة مجهولة"
                            val rCode = parts.getOrNull(1) ?: "----"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(2.dp, GoldYell, RoundedCornerShape(10.dp))
                                    .clickable { viewModel.joinLanHost(ip, playerNameInput) }
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Wifi, "Wifi game", tint = RedAccent)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(hostName, color = PapyrusText, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("رمز الاوضة: $rCode | IP: $ip", color = PapyrusTextSecondary, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Icon(Icons.Default.ArrowForward, "Join", tint = GoldYell)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4A1008)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("رجوع", color = GoldShine)
            }
        }
    }
}

// ==========================================
// 3. ROLE REVEAL SCREEN
// ==========================================
@Composable
fun RoleRevealScreen(viewModel: GameViewModel, state: RoomState) {
    val activePassPlayer = state.players.getOrNull(state.activePassPlayerIndex) ?: return
    val char = activePassPlayer.character ?: return
    var revealed by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "كشف الهوية السرية")
            Spacer(modifier = Modifier.height(12.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 112L) {
                if (!revealed) {
                    Icon(Icons.Default.Lock, contentDescription = "Secret Identity Locked", tint = RedAccent, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "دور اللاعب الحالي:", color = PapyrusTextSecondary, fontSize = 15.sp)
                    Text(text = activePassPlayer.name, color = Color(0xFF4A1008), fontSize = 26.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "ادي الموبايل لـ ${activePassPlayer.name} ومحدش يبص غيره، وبعدين اضغط الزرار تحت عشان تشوف ملفك السري.", color = PapyrusText, fontSize = 15.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.weight(1f))
                    Button(
                        onClick = { revealed = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("reveal_secret_button"),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(text = "أنا جاهز.. اكشف ملفي السري 🔍", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                } else {
                    Box(modifier = Modifier.size(72.dp).background(if (activePassPlayer.isMafia) Color(0x23E63946) else Color(0x1F2A9D8F), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(imageVector = if (activePassPlayer.isMafia) Icons.Default.Warning else Icons.Default.Security, contentDescription = "Role icon", tint = if (activePassPlayer.isMafia) RedAccent else InnocentAccent, modifier = Modifier.size(44.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "ملفك السري يا ${activePassPlayer.name}", color = PapyrusTextSecondary, fontSize = 14.sp)
                    Text(text = if (activePassPlayer.isMafia) "أنت الـمُــجرِم 🩸" else "أنت بـريء  🔍", color = if (activePassPlayer.isMafia) RedAccent else InnocentAccent, fontSize = 28.sp, fontWeight = FontWeight.Black, modifier = Modifier.testTag("role_text_reveal"))
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x0C000000)), border = BorderStroke(1.dp, Color(0x2B2C1E14))) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "شخصيتك: ${char.name}", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text(text = "المهنة: ${char.occupation} | السن: ${char.age}", color = PapyrusText, fontSize = 14.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (activePassPlayer.isMafia) Color(0x1AE63946) else Color(0x1A2A9D8F))) {
                        Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "الدافع / المهمة:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = if (activePassPlayer.isMafia) char.hiddenMotive else "انت برئ حاول تكتشف المجرم الحقيقي !!", color = PapyrusText, fontSize = 15.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.confirmSecretsRevealed(); revealed = false },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_reveal_advance"),
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Text(text = if (state.activePassPlayerIndex < state.players.size - 1) "خبي ملفك وهات اللي بعده" else " ادخل على تفاصيل القضية", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. CASE INTRO / DETAILS SCREEN
// ==========================================
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CaseIntroScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val isTablet = this.maxWidth > 700.dp
        val landscape = isLandscape(this.maxWidth, this.maxHeight)
        if (landscape || isTablet) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ParchmentHeaderBanner(text = "تفاصيل الجريمة")
                    Spacer(modifier = Modifier.height(10.dp))
                    ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
                        Text(text = currentCase.title, color = Color(0xFF7A1B0C), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(10.dp))
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                                Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                            Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                                Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "ملخص القضية والتقرير الجنائي:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = currentCase.description, color = PapyrusText, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.advanceFromCaseIntro() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("start_investigation_action_button")
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Investigate clues", tint = GoldShine)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Empty spacer/side view
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "تفاصيل الجريمة")
                Spacer(modifier = Modifier.height(10.dp))
                ParchmentCard(modifier = Modifier.weight(1f), seed = 9991L) {
                    Text(text = currentCase.title, color = Color(0xFF7A1B0C), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text("المكان: ${currentCase.location}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                        Box(modifier = Modifier.weight(1f).background(Color(0x0C000000), RoundedCornerShape(8.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
                            Text("الضحية: ${currentCase.victim}", color = PapyrusText, fontSize = 11.sp, textAlign = TextAlign.Center)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = "ملخص القضية والتقرير الجنائي:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = currentCase.description, color = PapyrusText, fontSize = 13.sp, lineHeight = 19.sp, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.advanceFromCaseIntro() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("start_investigation_action_button")
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Investigate clues", tint = GoldShine)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("ابدأ التحقيق ومراجعة الأدلة 🔎", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// 5. EVIDENCE SCREEN
// ==========================================
@Composable
fun EvidenceScreen(viewModel: GameViewModel, state: RoomState) {
    val currentCase = state.currentCase ?: return
    val clueIndex = state.currentEvidenceIndex
    val clue = currentCase.evidenceList.getOrNull(clueIndex) ?: return

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val landscape = isLandscape(this.maxWidth, this.maxHeight)
        val isTablet = this.maxWidth > 700.dp

        if (landscape || isTablet) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    ParchmentHeaderBanner(text = "الدليل الجنائي ${clueIndex + 1} من ${currentCase.evidenceList.size}")
                    Spacer(modifier = Modifier.height(12.dp))
                    ParchmentCard(modifier = Modifier.weight(1f), seed = (clueIndex + 10).toLong()) {
                        Box(modifier = Modifier.size(90.dp).background(Color(0xFF35120D), CircleShape).border(2.dp, GoldShine, CircleShape).shadow(4.dp, CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Search, contentDescription = "Evidence Seal", tint = GoldShine, modifier = Modifier.size(48.dp))
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(text = clue.title, color = RedAccent, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = clue.description, color = PapyrusText, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.nextEvidenceOrDiscussion() },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("next_evidence_action_button")
                    ) {
                        Text(text = if (clueIndex < currentCase.evidenceList.size - 1) "الدليل التالي ➡️" else "اقفل المحضر وادخل للنقاش 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "الدليل الجنائي ${clueIndex + 1} من ${currentCase.evidenceList.size}")
                Spacer(modifier = Modifier.height(12.dp))
                ParchmentCard(modifier = Modifier.weight(1f), seed = (clueIndex + 10).toLong()) {
                    Box(modifier = Modifier.size(90.dp).background(Color(0xFF35120D), CircleShape).border(2.dp, GoldShine, CircleShape).shadow(4.dp, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Search, contentDescription = "Evidence Seal", tint = GoldShine, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = clue.title, color = RedAccent, fontSize = 20.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = clue.description, color = PapyrusText, fontSize = 14.sp, lineHeight = 20.sp, textAlign = TextAlign.Center, modifier = Modifier.verticalScroll(rememberScrollState()).weight(1f))
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { viewModel.nextEvidenceOrDiscussion() },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("next_evidence_action_button")
                ) {
                    Text(text = if (clueIndex < currentCase.evidenceList.size - 1) "الدليل التالي ➡️" else "اقفل المحضر وادخل للنقاش 🗣️", color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

// ==========================================
// 6. DISCUSSION SCREEN
// ==========================================
@Composable
fun DiscussionScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val timeLeft = state.discussionTimeLeft
    val totalTime = state.settings.discussionTimeMinutes * 60
    val progress = if (totalTime > 0) timeLeft.toFloat() / totalTime.toFloat() else 0f
    val isHost = state.hostId == viewModel.myPlayerId.value
    val suspects = state.players.filter { it.isAlive }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val landscape = isLandscape(this.maxWidth, this.maxHeight)
        val isTablet = this.maxWidth > 700.dp

        if (landscape || isTablet) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ParchmentHeaderBanner(text = "طاولة نقاش المشتبهين")
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(color = Color(0x3B2C1E14), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 6.dp.toPx()))
                            drawArc(color = if (progress > 0.25f) DarkWoodButton else RedAccent, startAngle = -90f, sweepAngle = progress * 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Timer, "Timer clock", tint = if (progress > 0.25f) DarkWoodButton else RedAccent, modifier = Modifier.size(24.dp))
                            val mins = timeLeft / 60
                            val secs = timeLeft % 60
                            Text(text = String.format("%02d:%02d", mins, secs), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                        }
                    }
                }
                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp)
                    ) {
                        items(suspects, key = { it.id }) { candidate ->
                            val isClickSuspected = state.suspectedPlayerIds.contains(candidate.id)
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 135.dp)
                                    .background(Color(0xFFF2E6D0), RoundedCornerShape(12.dp))
                                    .border(2.dp, if (isClickSuspected) RedAccent else Color(0x3D2C1E14), RoundedCornerShape(12.dp))
                                    .clickable { if (isHost) viewModel.togglePlayerSuspicion(candidate.id) }
                                    .padding(8.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Box(modifier = Modifier.size(44.dp).background(if (isClickSuspected) RedAccent else DarkWoodButton, CircleShape), contentAlignment = Alignment.Center) {
                                        Text(text = candidate.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0x3B000000), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 3.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال",
                                            color = if (isClickSuspected) Color.Black else Color.White,
                                            fontSize = 8.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    ParchmentCard(modifier = Modifier.wrapContentHeight(), seed = 771L) {
                        Text(
                            text = "تناقشوا في القضية .....القاعدة المهمة الجميع متهم خلي بالك",
                            color = PapyrusTextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    if (isHost) {
                        Button(
                            onClick = { viewModel.playButtonClick(); viewModel.forceAdvanceToVoting() },
                            colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("skip_discussion_button")
                        ) {
                            Icon(Icons.Default.HowToVote, "Go to voting ballot", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("إنهاء النقاش والانتقال للتصويت السري 🗳️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    } else {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x242C1E14))) {
                            Text(text = "تناقشوا بحرية.. المضيف سينقلكم للتصويت عند انتهاء الوقت أو يدويًا.", color = PapyrusText, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "طاولة نقاش المشتبهين")
                Spacer(modifier = Modifier.height(16.dp))
                Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawArc(color = Color(0x3B2C1E14), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 6.dp.toPx()))
                        drawArc(color = if (progress > 0.25f) DarkWoodButton else RedAccent, startAngle = -90f, sweepAngle = progress * 360f, useCenter = false, style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round))
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Timer, "Timer clock", tint = if (progress > 0.25f) DarkWoodButton else RedAccent, modifier = Modifier.size(24.dp))
                        val mins = timeLeft / 60
                        val secs = timeLeft % 60
                        Text(text = String.format("%02d:%02d", mins, secs), color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(suspects, key = { it.id }) { candidate ->
                        val isClickSuspected = state.suspectedPlayerIds.contains(candidate.id)
                        Box(
                            modifier = Modifier
                                .size(width = 110.dp, height = 135.dp)
                                .background(Color(0xFFF2E6D0), RoundedCornerShape(12.dp))
                                .border(2.dp, if (isClickSuspected) RedAccent else Color(0x3D2C1E14), RoundedCornerShape(12.dp))
                                .clickable { if (isHost) viewModel.togglePlayerSuspicion(candidate.id) }
                                .padding(8.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Box(modifier = Modifier.size(44.dp).background(if (isClickSuspected) RedAccent else DarkWoodButton, CircleShape), contentAlignment = Alignment.Center) {
                                    Text(text = candidate.avatarId.toString(), color = GoldShine, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .background(Color(0x3B000000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 3.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = if (isClickSuspected) "متهم ⚠️" else "قيد السؤال",
                                        color = if (isClickSuspected) Color.Black else Color.White,
                                        fontSize = 8.sp
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                ParchmentCard(modifier = Modifier.wrapContentHeight(), seed = 771L) {
                    Text(
                        text = "تناقشوا في القضية .....القاعدة المهمة الجميع متهم خلي بالك",
                        color = PapyrusTextSecondary,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                if (isHost) {
                    Button(
                        onClick = { viewModel.playButtonClick(); viewModel.forceAdvanceToVoting() },
                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("skip_discussion_button")
                    ) {
                        Icon(Icons.Default.HowToVote, "Go to voting ballot", tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("إنهاء النقاش والانتقال للتصويت السري 🗳️", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                } else {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x242C1E14))) {
                        Text(text = "تناقشوا بحرية.. المضيف سينقلكم للتصويت عند انتهاء الوقت أو يدويًا.", color = PapyrusText, fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(12.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// 7. VOTING SCREEN
// ==========================================
@Composable
fun VotingScreen(viewModel: GameViewModel, state: RoomState) {
    val context = LocalContext.current
    val candidates = state.players.filter { it.isAlive }
    var selectedTargetId by remember { mutableStateOf("") }
    val isPassPlay = state.mode == "PASS_PLAY"
    val activeVoter = if (isPassPlay) state.players.getOrNull(state.activeVotingPlayerIndex) else state.players.find { it.id == viewModel.myPlayerId.value }
    val modeText = if (isPassPlay) "دور اللاعب للتصويت السري:" else "صندوق الاقتراع الرقمي"

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val landscape = isLandscape(this.maxWidth, this.maxHeight)
        val isTablet = this.maxWidth > 700.dp

        if (landscape || isTablet) {
            Row(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
                    Spacer(modifier = Modifier.height(12.dp))
                    if (activeVoter != null) {
                        Text(text = modeText, color = PapyrusTextSecondary, fontSize = 14.sp)
                        Text(text = activeVoter.name, color = RedAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "اختر الشخص الذي تظن أنه المجرم الحقيقي بناءً على الأدلة والملفات الجنائية. تصويتك سري بالكامل ولن يراه أحد!", color = PapyrusText, fontSize = 13.sp, textAlign = TextAlign.Center)
                    }
                }
                Column(
                    modifier = Modifier.weight(1.5f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates, key = { it.id }) { candidate ->
                            val isSelected = candidate.id == selectedTargetId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp))
                                    .border(2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp))
                                    .clickable { selectedTargetId = candidate.id }
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.size(38.dp).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                    Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            if (selectedTargetId.isBlank()) Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                            else viewModel.submitVote(selectedTargetId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_vote_action_button")
                    ) {
                        Text("أأكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "صندوق التصويت والاتهامات")
                Spacer(modifier = Modifier.height(12.dp))
                if (activeVoter != null) {
                    Text(text = modeText, color = PapyrusTextSecondary, fontSize = 14.sp)
                    Text(text = activeVoter.name, color = RedAccent, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "اختر الشخص الذي تظن أنه المجرم الحقيقي بناءً على الأدلة والملفات الجنائية. تصويتك سري بالكامل ولن يراه أحد!", color = PapyrusText, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(candidates, key = { it.id }) { candidate ->
                        val isSelected = candidate.id == selectedTargetId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(if (isSelected) Color(0x3B6E1B10) else Color(0x0C000000), RoundedCornerShape(10.dp))
                                .border(2.dp, if (isSelected) RedAccent else Color(0x1F2C1E14), RoundedCornerShape(10.dp))
                                .clickable { selectedTargetId = candidate.id }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.size(38.dp).background(if (isSelected) RedAccent else Color(0xFF421D18), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(imageVector = if (isSelected) Icons.Default.Check else Icons.Default.Person, contentDescription = "Pick status target", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(text = candidate.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (selectedTargetId.isBlank()) Toast.makeText(context, "اختار حد تشك فيه الأول عشان تصوّت", Toast.LENGTH_SHORT).show()
                        else viewModel.submitVote(selectedTargetId)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(56.dp).testTag("submit_vote_action_button")
                ) {
                    Text("أأكد صوتك يلا", color = GoldShine, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                }
            }
        }
    }
}

// ==========================================
// 8. VOTE RESULT SCREEN
// ==========================================
@Composable
fun VoteResultScreen(viewModel: GameViewModel, state: RoomState) {
    val isHost = state.hostId == viewModel.myPlayerId.value
    val votesSummary = state.votesResultSummary

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "نتائج الفرز والجرائم")
            Spacer(modifier = Modifier.height(16.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 882L) {
                Icon(Icons.Default.Analytics, "Stats results", tint = DarkWoodButton, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(12.dp))
                Text(text = "محضر الفرز القضائي للأصوات:", color = Color(0xFF4A1008), fontWeight = FontWeight.Black, fontSize = 18.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    if (votesSummary.isEmpty()) Text("لم يتم الإدلاء بأي أصوات.", color = Color.Gray, fontSize = 14.sp)
                    else votesSummary.forEach { voteText -> Text(text = voteText, color = Color(0xFF2C1E14), fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 2.dp)) }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            if (isHost) {
                Button(onClick = { viewModel.playButtonClick(); viewModel.confirmVoteResultAndProceed() }, colors = ButtonDefaults.buttonColors(containerColor = RedAccent), modifier = Modifier.fillMaxWidth().height(56.dp).testTag("confirm_vote_result_button"), shape = RoundedCornerShape(12.dp)) {
                    Text(text = if (state.tiedVotePlayers.isNotEmpty()) "بدء جولة حسم التعادل" else "متابعة مسار التحقيق", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            } else {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x3D2C1E14)), shape = RoundedCornerShape(12.dp)) {
                    Text(text = "في انتظار المضيف لمتابعة القضية...", color = PapyrusBgLight, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(16.dp))
                }
            }
        }
    }
}

// ==========================================
// 9. JURY SCREEN
// ==========================================
@Composable
fun JuryScreen(viewModel: GameViewModel, state: RoomState) {
    val myPlayer = state.players.find { it.id == viewModel.myPlayerId.value }
    val isAlive = myPlayer?.isAlive ?: true
    val tiedPlayers = state.players.filter { state.tiedVotePlayers.contains(it.id) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        val landscape = isLandscape(this.maxWidth, this.maxHeight)
        val isTablet = this.maxWidth > 700.dp
        if (isAlive) {
            if (landscape || isTablet) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "هيئة المحلفين بتصوّت دلوقتي...",
                            color = PapyrusBgLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "مصيرك وصاحبك الأخير بين إيدين اللاعبين اللي خرجوا! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                MysteryBackground {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "هيئة المحلفين بتصوّت دلوقتي...",
                            color = PapyrusBgLight,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "مصيرك وصاحبك الأخير بين إيدين اللاعبين اللي خرجوا! مين هيتبرأ ومين هيدان؟ تفتكر هيختاروا صح؟",
                            color = Color.LightGray,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ParchmentHeaderBanner(text = "هيئة المحلفين العليا ⚖️")
                Spacer(modifier = Modifier.height(10.dp))
                ParchmentCard(modifier = Modifier.weight(1f), seed = 88L) {
                    Box(
                        modifier = Modifier.size(80.dp).background(Color(0x3B6E1B10), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Gavel, "Gavel judge", tint = RedAccent, modifier = Modifier.size(48.dp))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "!!! لا تقلقوا ولكن احذروا !!!",
                        color = Color(0xFF6E1D10),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (tiedPlayers.isNotEmpty()) {
                        Text(
                            text = "بما إنك ميت دلوقتي برة القضية، صوتك هو العدل! اختار مين من الاتنين دول تدينه بصفة نهائية لإنهاء التعادل:",
                            color = PapyrusText,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(tiedPlayers, key = { it.id }) { suspect ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0x0C000000), RoundedCornerShape(10.dp))
                                        .border(1.dp, Color(0x3D2C1E14), RoundedCornerShape(10.dp))
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(suspect.name, color = PapyrusText, fontWeight = FontWeight.Bold, fontSize = 18.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        suspect.character?.let { Text("الشخصية: ${it.name} | المهنة: ${it.occupation}", color = PapyrusTextSecondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                                    }
                                    Button(
                                        onClick = { viewModel.submitJuryVote(suspect.id) },
                                        colors = ButtonDefaults.buttonColors(containerColor = RedAccent)
                                    ) {
                                        Text("إدانة", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "تم جمع كافة استنتاجات اللاعبين بنجاح. سنعلن النتيجة الآن!",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. ENDGAME SCREEN
// ==========================================
@Composable
fun EndgameScreen(viewModel: GameViewModel, state: RoomState) {
    val mafia = state.players.find { it.isMafia }
    val winnerText = state.endgameWinnerSummary

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
    ) {
        val padding = responsivePadding(this.maxWidth)
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            ParchmentHeaderBanner(text = "نهاية التحقيق الجنائي")
            Spacer(modifier = Modifier.height(10.dp))
            ParchmentCard(modifier = Modifier.weight(1f), seed = 1209L) {
                Box(modifier = Modifier.size(76.dp).background(Color(0x28GoldShine.toArgb()), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.EmojiEvents, "Victory cup trophy", tint = GoldYell, modifier = Modifier.size(48.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = winnerText, color = Color(0xFF4A1008), fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, modifier = Modifier.testTag("endgame_victory_text"))
                Spacer(modifier = Modifier.height(10.dp))
                if (mafia != null) {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x1CE63946)), border = BorderStroke(1.dp, Color(0xFFE63946)), shape = RoundedCornerShape(8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(text = "المجرم الحقيقي: ${mafia.name}", color = Color(0xFF3B6E1B10), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, modifier = Modifier.testTag("criminal_character_name"))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("العمر: ${mafia.character?.age ?: 30} سنة | المهنة: ${mafia.character?.occupation ?: "مجهول"}", color = PapyrusText, fontSize = 15.sp)
                            Text("المظهر والطباع: ${mafia.character?.traits ?: ""}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            Text("المستوى الاجتماعي: ${mafia.character?.socialStatus ?: "متوسط الحال"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            Text("علاقته بالضحية: ${mafia.character?.relationshipToVictim ?: "غامضة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            Text("علاقته بالمشتبهين: ${mafia.character?.relationshipToOtherSuspects ?: "منافسة"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                            Text("السجل الجنائي: ${mafia.character?.relevantHistory ?: "خالي من السوابق"}", color = PapyrusTextSecondary, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0x0C000000))) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "جدول أدوار اللاعبين الصادقة:", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        state.players.forEach { p ->
                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(text = if (p.isMafia) "مجرم" else "بريء ", color = if (p.isMafia) RedAccent else InnocentAccent, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(text = "${p.name} (${p.character?.name ?: ""})", color = PapyrusTextSecondary, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(padding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
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
}

// ==========================================
// 10. SETTINGS & CONVENTIONS DIALOG
// ==========================================
@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismissRequest: () -> Unit) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember { mutableStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember { mutableStateOf(state.settings.votingTimeMinutes) }
    var soundEnabled by remember { mutableStateOf(state.settings.isSoundEnabled) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "إعدادات اللعبة والقوانين ⚙️", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text(text = "وقت النقاش الجماعي ($discTimeMins دقائق):", color = PapyrusText)
                    Slider(value = discTimeMins.toFloat(), onValueChange = { discTimeMins = it.toInt() }, valueRange = 1f..10f, steps = 8, colors = SliderDefaults.colors(thumbColor = DarkWoodButton, activeTrackColor = DarkWoodButton))
                }
                Column {
                    Text(text = "وقت التصويت ($voteTimeMins دقائق):", color = PapyrusText)
                    Slider(value = voteTimeMins.toFloat(), onValueChange = { voteTimeMins = it.toInt() }, valueRange = 1f..5f, steps = 3, colors = SliderDefaults.colors(thumbColor = DarkWoodButton, activeTrackColor = DarkWoodButton))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "المؤثرات الصوتية والموسيقى تصويرية:", color = PapyrusText)
                    Switch(checked = soundEnabled, onCheckedChange = { soundEnabled = it }, colors = SwitchDefaults.colors(checkedThumbColor = GoldShine, checkedTrackColor = DarkWoodButton))
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.updateRoomSettings(RoomSettings(discussionTimeMinutes = discTimeMins, votingTimeMinutes = voteTimeMins, isSoundEnabled = soundEnabled)); onDismissRequest() }, colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)) {
                Text(text = "حفظ التعديلات", color = GoldShine)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = "إلغاء", color = Color.Gray)
            }
        },
        containerColor = PapyrusBg
    )
}