package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.MysteryBackground
import com.example.ui.theme.DarkBg

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun GameNavigation(viewModel: GameViewModel) {
    val state by viewModel.roomState.collectAsState()
    val context = LocalContext.current

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