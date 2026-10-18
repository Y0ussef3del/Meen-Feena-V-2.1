package com.example.game.viewmodel

import androidx.lifecycle.viewModelScope
import com.example.game.model.GamePhase
import com.example.game.model.Player
import com.example.game.model.RoomState
import com.example.game.network.OnlineManager
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*

internal fun GameViewModel.setupOnlineListeners() {
    viewModelScope.launch {
        OnlineManager.incomingCommands.collect { (sourceId, msg) ->
            handleIncomingOnlineMessage(sourceId, msg)
        }
    }
}

internal fun GameViewModel.handleIncomingOnlineMessage(source: String, msg: String) {
    try {
        val json = JSONObject(msg)
        val type = json.optString("type")
        when (type) {
            "JOIN" -> {
                val pName = json.optString("playerName", "لاعب")
                val deviceId = json.optString("deviceId", UUID.randomUUID().toString())
                addOnlineClientPlayer(deviceId, pName)
            }
            "REVEAL_SECRET" -> {
                val playerId = json.optString("playerId", "")
                handlePlayerRevealedRole(playerId)
            }
            "VOTE" -> {
                val voterId = json.optString("voterId", "")
                val targetId = json.optString("targetId", "")
                if (voterId.isNotEmpty() && targetId.isNotEmpty()) castVote(voterId, targetId)
            }
            "JURY_VOTE" -> {
                val voterId = json.optString("voterId", "")
                val targetId = json.optString("targetId", "")
                if (voterId.isNotEmpty() && targetId.isNotEmpty()) castJuryVote(voterId, targetId)
            }
            "CLIENT_LEAVE" -> {
                val deviceId = json.optString("deviceId", "")
                if (deviceId.isNotEmpty()) {
                    webRtcManager.closePeerConnection(deviceId)
                    removePlayerFromLobby(deviceId)
                }
            }
            "STATE_UPDATE" -> {
                val stateJsonStr = json.optString("data", "")
                if (stateJsonStr.isNotEmpty()) {
                    val updatedState = RoomState.fromSharedJsonString(stateJsonStr)
                    _roomState.update { current -> updatedState.copy(roomId = current.roomId) }
                }
            }
            "HOST_DISCONNECTED" -> {
                OnlineManager.disconnectFromHost()
                resetToMainMenu()
            }
            "WEBRTC_OFFER" -> {
                val senderId = json.optString("senderId", source)
                val sdp = json.optString("sdp", "")
                if (sdp.isNotEmpty()) {
                    webRtcManager.setMyPlayerId(myPlayerId.value)
                    webRtcManager.handleOffer(senderId, sdp)
                }
            }
            "WEBRTC_ANSWER" -> {
                val senderId = json.optString("senderId", source)
                val sdp = json.optString("sdp", "")
                if (sdp.isNotEmpty()) {
                    webRtcManager.setMyPlayerId(myPlayerId.value)
                    webRtcManager.handleAnswer(senderId, sdp)
                }
            }
            "WEBRTC_ICE" -> {
                val senderId = json.optString("senderId", source)
                val sdpMid = json.optString("sdpMid", "")
                val sdpMLineIndex = json.optInt("sdpMLineIndex", 0)
                val candidate = json.optString("candidate", "")
                if (candidate.isNotEmpty()) {
                    webRtcManager.handleCandidate(senderId, sdpMid, sdpMLineIndex, candidate)
                }
            }
        }
    } catch (e: Exception) {}
}

fun GameViewModel.setupPassAndPlayGame() {
    stopTimer()
    val currentSettings = _roomState.value.settings
    val caseToKeep = _roomState.value.currentCase

    _roomState.value = RoomState(
        roomId = "PASS_AND_PLAY_ROOM",
        mode = "PASS_AND_PLAY",
        hostId = "LOCAL_HOST",
        phase = GamePhase.LOBBY,
        players = listOf(),
        settings = currentSettings,
        heartsCount = getLocalHeartsCount(),
        currentCase = caseToKeep
    )
    myPlayerId.value = "p1"
}

fun GameViewModel.addLocalLobbyPlayer(name: String) {
    if (name.isBlank()) return
    playSelection()
    _roomState.update { current ->
        if (current.players.size >= 6) return@update current
        val currentPlayers = current.players.toMutableList()
        val newId = "p${currentPlayers.size + 1}"
        currentPlayers.add(Player(newId, name, avatarId = (currentPlayers.size % 6) + 1))
        current.copy(players = currentPlayers)
    }
}

fun GameViewModel.removePlayerFromLobby(id: String) {
    playWarning()
    _roomState.update { current ->
        val updatedPlayers = current.players.filter { it.id != id }
        val updatedState = current.copy(players = updatedPlayers)
        if (updatedState.mode == "ONLINE") {
            OnlineManager.broadcastStateToClients(updatedState)
        }
        updatedState
    }
}

fun GameViewModel.startLanHost(hostPlayerName: String) {
    startOnlineHost(hostPlayerName)
}

fun GameViewModel.startOnlineHost(hostPlayerName: String) {
    stopTimer()
    playSuccess()
    val deviceId = OnlineManager.localDeviceId
    myPlayerId.value = deviceId
    myPlayerName.value = hostPlayerName
    webRtcManager.setMyPlayerId(deviceId)
    val currentSettings = _roomState.value.settings
    val roomCode = (kotlin.random.Random.nextInt(90000) + 10000).toString()
    val newState = RoomState(
        roomId = roomCode,
        mode = "ONLINE",
        hostId = deviceId,
        phase = GamePhase.LOBBY,
        players = listOf(Player(deviceId, hostPlayerName, avatarId = 1)),
        settings = currentSettings,
        heartsCount = getLocalHeartsCount()
    )
    _roomState.value = newState
    OnlineManager.startHost(hostPlayerName, roomCode)
    OnlineManager.broadcastStateToClients(newState)
}

fun GameViewModel.joinLanHostByCode(roomCode: String, playerName: String): Boolean {
    if (roomCode.length != 5) return false
    joinOnlineRoom(roomCode, playerName)
    return true
}

fun GameViewModel.joinLanHost(ip: String, playerName: String) {
    joinOnlineRoom(ip, playerName)
}

fun GameViewModel.joinOnlineRoom(roomCode: String, playerName: String) {
    stopTimer()
    playSelection()
    val deviceId = OnlineManager.localDeviceId
    myPlayerId.value = deviceId
    myPlayerName.value = playerName
    webRtcManager.setMyPlayerId(deviceId)
    val currentSettings = _roomState.value.settings
    _roomState.value = RoomState(roomId = roomCode, mode = "ONLINE", phase = GamePhase.LOBBY, settings = currentSettings)
    OnlineManager.connectToHost(roomCode, playerName, deviceId)
}

internal fun GameViewModel.addOnlineClientPlayer(deviceId: String, name: String) {
    _roomState.update { current ->
        val currentPlayers = current.players.toMutableList()
        val existingIndex = currentPlayers.indexOfFirst { it.id == deviceId }
        if (existingIndex >= 0) {
            currentPlayers[existingIndex] = currentPlayers[existingIndex].copy(isConnected = true)
        } else {
            if (currentPlayers.size >= 6) return@update current
            currentPlayers.add(Player(deviceId, name, avatarId = (currentPlayers.size % 6) + 1))
        }
        val updatedState = current.copy(players = currentPlayers)
        OnlineManager.broadcastStateToClients(updatedState)
        updatedState
    }
}