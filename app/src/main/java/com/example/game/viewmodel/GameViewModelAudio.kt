package com.example.game.viewmodel

import com.example.game.audio.MysteryAudioPlayer

fun GameViewModel.playButtonClick() { safePlaySound { MysteryAudioPlayer.playSelection() } }
fun GameViewModel.playSelection() { safePlaySound { MysteryAudioPlayer.playSelection() } }
fun GameViewModel.playSuccess() { safePlaySound { MysteryAudioPlayer.playSuccess() } }
fun GameViewModel.playError() { safePlaySound { MysteryAudioPlayer.playError() } }
fun GameViewModel.playWarning() { safePlaySound { MysteryAudioPlayer.playWarning() } }
fun GameViewModel.playVoteSound() { safePlaySound { MysteryAudioPlayer.playVote() } }
fun GameViewModel.playTransitionSound() { safePlaySound { MysteryAudioPlayer.playTransition() } }
fun GameViewModel.playRevealSound() { safePlaySound { MysteryAudioPlayer.playReveal() } }

internal inline fun GameViewModel.safePlaySound(action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {}
}