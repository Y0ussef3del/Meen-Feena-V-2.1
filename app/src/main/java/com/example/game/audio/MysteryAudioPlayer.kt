package com.example.game.audio

import android.content.Context
import android.media.MediaPlayer
import com.example.R // Make sure this matches your package structure

object MysteryAudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var musicVolume = 0.5f

    // 1. Initialize and start continuous background looping music
    fun startMusic(context: Context) {
        if (mediaPlayer == null) {
            try {
                // Automatically targets res/raw/music_background.raw/.mp3/.wav
                mediaPlayer = MediaPlayer.create(context, R.raw.music_background).apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else if (!mediaPlayer!!.isPlaying) {
            mediaPlayer?.start()
        }
    }

    fun stopMusic() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }
}