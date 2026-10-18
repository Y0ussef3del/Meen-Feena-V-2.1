package com.example.game.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.game.audio.MysteryAudioPlayer
import com.example.game.data.CaseRepository
import com.example.game.model.*
import com.example.game.network.OnlineManager
import com.example.game.network.WebRtcManager
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    internal val TAG = "GameViewModel"

    internal val _roomState = MutableStateFlow(RoomState())
    val roomState: StateFlow<RoomState> = _roomState.asStateFlow()

    val myPlayerId = MutableStateFlow(OnlineManager.localDeviceId)
    val myPlayerName = MutableStateFlow("نمبر 1")

    internal val _completedCaseTitles = MutableStateFlow<Set<String>>(emptySet())
    val completedCaseTitles: StateFlow<Set<String>> = _completedCaseTitles.asStateFlow()

    val newLobbyPlayerName = MutableStateFlow("")
    internal var timerJob: Job? = null

    internal var rewardedAd: RewardedAd? = null
    internal var isAdLoading = false

    internal var interstitialAd: InterstitialAd? = null
    internal var isInterstitialLoading = false

    internal val sharedPreferences: SharedPreferences = application.getSharedPreferences("GamePrefs", Context.MODE_PRIVATE)

    internal val webRtcManager = WebRtcManager(application)
    val voiceChatStatus: StateFlow<String> = webRtcManager.voiceStatus
    val isMicMuted = MutableStateFlow(false)
    val mutedPlayersState = MutableStateFlow<Set<String>>(emptySet())
    internal var isVoiceChatActive = false

    companion object {
        const val MAX_HEARTS = 3
    }

    init {
        try {
            CaseRepository.init(application)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing CaseRepository", e)
        }
        val savedCompleted = sharedPreferences.getStringSet("completed_case_titles", emptySet()) ?: emptySet()
        _completedCaseTitles.value = savedCompleted

        webRtcManager.setMyPlayerId(myPlayerId.value)
        setupWebRtcCallbacks()
        setupOnlineListeners()
        checkAndResetDailyHearts()
        loadRewardedAdInternal()
        loadInterstitialAdInternal()

        viewModelScope.launch {
            OnlineManager.clientConnectionError.collect { error ->
                if (!error.isNullOrEmpty()) {
                    _roomState.update { current ->
                        if (current.mode == "ONLINE" && current.hostId != myPlayerId.value) {
                            RoomState(settings = current.settings)
                        } else current
                    }
                }
            }
        }

        viewModelScope.launch {
            var lastVolume: Float? = null
            roomState.collect { state ->
                if (lastVolume != state.settings.volume) {
                    lastVolume = state.settings.volume
                    try {
                        MysteryAudioPlayer.setVolume(state.settings.volume)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error setting volume", e)
                    }
                }
            }
        }

        viewModelScope.launch {
            roomState.collect { state ->
                if (state.mode == "ONLINE" && state.phase == GamePhase.DISCUSSION) {
                    startVoiceChatIfPermitted()
                } else {
                    if (isVoiceChatActive) {
                        isVoiceChatActive = false
                        try {
                            webRtcManager.stopVoiceChat()
                        } catch (e: Exception) {
                            Log.e(TAG, "Prevented crash during phase change", e)
                        }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopTimer()
        isVoiceChatActive = false
        webRtcManager.release()
        OnlineManager.stopHost()
        OnlineManager.disconnectFromHost()
    }
}