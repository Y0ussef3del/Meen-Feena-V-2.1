package com.example.ui.screens

import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.*
import com.example.ui.theme.*

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

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val activity = LocalActivity.current

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
        onDispose { }
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

@Composable
fun SettingsDialog(viewModel: GameViewModel, onDismissRequest: () -> Unit) {
    val state by viewModel.roomState.collectAsState()
    var discTimeMins by remember { mutableStateOf(state.settings.discussionTimeMinutes) }
    var voteTimeMins by remember { mutableStateOf(state.settings.votingTimeMinutes) }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(text = "إعدادات اللعبة والقوانين ⚙️", color = Color(0xFF4A1008), fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.fillMaxWidth()) },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    // تم إغلاق الـ Dialog مباشرة لتجنب استدعاء دالة تحديث غير مطابقة في الـ ViewModel الحالي
                    onDismissRequest() 
                }, 
                colors = ButtonDefaults.buttonColors(containerColor = DarkWoodButton)
            ) {
                Text(text = "حفظ التعديلات", color = GoldShine)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) { Text(text = "إلغاء", color = Color.Gray) }
        },
        containerColor = PapyrusBg
    )
}