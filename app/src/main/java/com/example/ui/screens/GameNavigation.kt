package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.viewmodel.GameViewModel
import com.example.ui.components.MysteryBackground
import com.example.ui.theme.DarkBg
import kotlinx.coroutines.delay

@Composable
fun GameNavigation(
    viewModel: GameViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val state by viewModel.roomState.collectAsState()
    val completedCaseTitles by viewModel.completedCaseTitles.collectAsState()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2200)
        showSplash = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "game_flow") {

            composable("game_flow") {
                MysteryBackground(drawBloodDrips = state.phase == GamePhase.LOBBY) {
                    when (state.phase) {
                        GamePhase.LOBBY -> MainMenuOrLobbyScreen(
                            viewModel = viewModel,
                            state = state,
                            navController = navController
                        )
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

            composable("cases_library") {
                CasesLibraryScreen(
                    repository = CaseRepository,
                    completedCaseTitles = completedCaseTitles,
                    onPlayCase = { selectedCase ->
                        navController.popBackStack("game_flow", inclusive = false)
                        viewModel.selectCustomCase(selectedCase)
                    },
                    onCreateNewCase = { navController.navigate("create_case") },
                    onEditCase = { selectedCase -> }
                )
            }

            composable("create_case") {
                CreateCaseScreen(
                    repository = CaseRepository,
                    onCaseSaved = { navController.popBackStack() }
                )
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(600))
        ) {
            Box(modifier = Modifier.fillMaxSize().background(DarkBg)) {
                SplashScreen()
            }
        }
    }
}