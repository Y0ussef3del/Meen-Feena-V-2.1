package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicVolume = 0.5f

    private var mediaPlayer: MediaPlayer? = null
    private var context: Context? = null
    private var isMusicEnabled = false

    fun init(appContext: Context) {
        context = appContext.applicationContext
    }

    fun playClick() = playSelection()
    fun playSelection() { /* ... implementation as before ... */ }
    fun playSuccess() { /* ... */ }
    fun playError() { /* ... */ }
    fun playWarning() { /* ... */ }
    fun playVote() { /* ... */ }
    fun playTransition() { /* ... */ }
    fun playTension() = playReveal()
    fun playReveal() { /* ... */ }

    private suspend fun playBufferStatic(buffer: ShortArray, sampleRate: Int) { /* ... */ }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    fun startMusic() {  // لا معاملات!
        val ctx = context ?: return
        if (!isMusicEnabled || musicVolume <= 0f) return
        if (mediaPlayer == null) {
            try {
                val resId = ctx.resources.getIdentifier("background_music", "raw", ctx.packageName)
                if (resId == 0) {
                    Log.w(TAG, "Background music not found in res/raw/")
                    return
                }
                mediaPlayer = MediaPlayer.create(ctx, resId)?.apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    start()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start music", e)
            }
        } else {
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {  // لا معاملات!
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    // للتحكم من ViewModel
    fun enableMusic(enabled: Boolean) {
        isMusicEnabled = enabled
        if (enabled && musicVolume > 0f) startMusic() else stopMusic()
    }
}