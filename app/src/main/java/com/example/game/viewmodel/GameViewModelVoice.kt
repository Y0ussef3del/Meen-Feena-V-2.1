package com.example.game.viewmodel

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.game.model.GamePhase
import com.example.game.network.OnlineManager
import org.json.JSONObject

internal fun GameViewModel.setupWebRtcCallbacks() {
    webRtcManager.onIceCandidateGenerated = { targetId, candidate ->
        val json = JSONObject().apply {
            put("type", "WEBRTC_ICE")
            put("senderId", myPlayerId.value)
            put("targetId", targetId)
            put("sdpMid", candidate.sdpMid ?: "")
            put("sdpMLineIndex", candidate.sdpMLineIndex)
            put("candidate", candidate.sdp)
        }.toString()
        OnlineManager.sendRtcSignal(targetId, json)
    }

    webRtcManager.onSdpGenerated = { targetId, sdp ->
        val typeStr = if (sdp.type == org.webrtc.SessionDescription.Type.OFFER) "WEBRTC_OFFER" else "WEBRTC_ANSWER"
        val json = JSONObject().apply {
            put("type", typeStr)
            put("senderId", myPlayerId.value)
            put("targetId", targetId)
            put("sdp", sdp.description)
        }.toString()
        OnlineManager.sendRtcSignal(targetId, json)
    }
}

fun GameViewModel.startVoiceChatIfPermitted() {
    val state = _roomState.value
    if (state.mode == "ONLINE" && state.phase == GamePhase.DISCUSSION) {
        val hasPermission = ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            webRtcManager.setMyPlayerId(myPlayerId.value)
            val remoteIds = state.players.map { it.id }

            if (!isVoiceChatActive) {
                isVoiceChatActive = true
                webRtcManager.startVoiceChat(state.roomId, myPlayerId.value, remoteIds)
            } else {
                webRtcManager.syncVoiceConnections(remoteIds)
            }
        }
    }
}

fun GameViewModel.toggleSelfMic() {
    val muted = webRtcManager.toggleSelfMic()
    isMicMuted.value = muted
}

fun GameViewModel.toggleMutePlayer(playerId: String) {
    webRtcManager.toggleMutePlayer(playerId)
    val mutedSet = mutedPlayersState.value.toMutableSet()
    if (webRtcManager.isPlayerMuted(playerId)) {
        mutedSet.add(playerId)
    } else {
        mutedSet.remove(playerId)
    }
    mutedPlayersState.value = mutedSet
}

fun GameViewModel.isPlayerMuted(playerId: String): Boolean {
    return webRtcManager.isPlayerMuted(playerId)
}