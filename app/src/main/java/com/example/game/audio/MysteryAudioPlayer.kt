package com.example.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*
import kotlin.math.sin

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicVolume = 0.5f

    // Background music player
    private var mediaPlayer: MediaPlayer? = null
    private var isMusicPrepared = false
    private var context: Context? = null

    // يجب استدعاء هذه الدالة من Application أو Activity لتعيين Context
    fun init(context: Context) {
        this.context = context.applicationContext
    }

    // 1. playClick / playSelection: short wooden click feedback
    fun playClick() {
        playSelection()
    }

    fun playSelection() {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * 0.04).toInt() // 40ms short tap
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 600.0 * (1.0 - t * 20.0).coerceAtLeast(0.2)
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / 0.04)
                    buffer[i] = (envelope * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing selection sound", e)
            }
        }
    }

    // 2. playSuccess: happy ascending sequence
    fun playSuccess() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.22 // 220ms total
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = when {
                        t < 0.07 -> 523.25 // C5
                        t < 0.14 -> 659.25 // E5
                        else -> 783.99 // G5
                    }
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / duration)
                    buffer[i] = (envelope * 22000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing success sound", e)
            }
        }
    }

    // 3. playError: low, harsh buzz
    fun playError() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.25 // 250ms total
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val isSilenced = t in 0.10..0.13
                    val wave = if (isSilenced) {
                        0.0
                    } else {
                        (sin(2.0 * Math.PI * 120.0 * t) + 0.5 * sin(2.0 * Math.PI * 240.0 * t))
                    }
                    val envelope = wave * (1.0 - t / duration)
                    buffer[i] = (envelope * 24000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing error sound", e)
            }
        }
    }

    // 4. playWarning: metallic double tone beep
    fun playWarning() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.15 // 150ms
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val wave = sin(2.0 * Math.PI * 440.0 * t) + 0.3 * sin(2.0 * Math.PI * 880.0 * t)
                    val envelope = wave * (1.0 - t / duration)
                    buffer[i] = (envelope * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing warning sound", e)
            }
        }
    }

    // 5. playVote: short thumping stamp sound
    fun playVote() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.12 // 120ms
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 120.0 * (1.0 - t * 8.0).coerceAtLeast(0.3)
                    val envelope = sin(2.0 * Math.PI * freq * t) * (1.0 - t / duration)
                    buffer[i] = (envelope * 25000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing vote sound", e)
            }
        }
    }

    // 6. playTransition: suspenseful rising pitch swoop
    fun playTransition() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.45 // 450ms
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = 150.0 + 650.0 * (t / duration) * (t / duration)
                    val volEnvelope = if (t < 0.1) t / 0.1 else (1.0 - t / duration)
                    val wave = sin(2.0 * Math.PI * freq * t) * volEnvelope
                    buffer[i] = (wave * 20000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing transition sound", e)
            }
        }
    }

    // 7. playReveal / playTension: dramatic dissonance and sub-bass stinger
    fun playTension() {
        playReveal()
    }

    fun playReveal() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.8 // 800ms
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val chord = sin(2.0 * Math.PI * 220.0 * t) +
                                0.7 * sin(2.0 * Math.PI * 311.13 * t) +
                                0.5 * sin(2.0 * Math.PI * 55.0 * t)
                    val envelope = chord * Math.exp(-3.5 * t)
                    buffer[i] = (envelope * 24000.0 * musicVolume).toInt().toShort()
                }
                playBufferStatic(buffer, sampleRate)
            } catch (e: Throwable) {
                Log.e(TAG, "Error playing reveal sound", e)
            }
        }
    }

    // 8. Static play helper
    private suspend fun playBufferStatic(buffer: ShortArray, sampleRate: Int) {
        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(buffer.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(buffer, 0, buffer.size)
        audioTrack.play()
        delay((buffer.size * 1000L / sampleRate) + 50L)
        try { audioTrack.release() } catch (ex: Throwable) {}
    }

    // --- Background Music Methods (NEW) ---
    @Synchronized
    fun startMusic() {
        val ctx = context ?: run {
            Log.e(TAG, "Context not initialized. Call init() first.")
            return
        }
        // إذا كانت الموسيقى مفعلة والمشغل جاهز، ابدأ التشغيل
        if (musicVolume > 0f && mediaPlayer == null) {
            try {
                // استخدام resource ID للموسيقى، يجب وضع ملف mp3 في res/raw/background_music.mp3
                val musicResId = ctx.resources.getIdentifier("background_music", "raw", ctx.packageName)
                if (musicResId == 0) {
                    Log.w(TAG, "Background music file not found in res/raw/")
                    return
                }
                mediaPlayer = MediaPlayer.create(ctx, musicResId)?.apply {
                    isLooping = true
                    setVolume(musicVolume, musicVolume)
                    prepare()
                    start()
                    isMusicPrepared = true
                    Log.d(TAG, "Background music started")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start background music", e)
                mediaPlayer = null
            }
        } else if (mediaPlayer?.isPlaying == true && musicVolume <= 0f) {
            stopMusic()
        } else if (mediaPlayer?.isPlaying == false && musicVolume > 0f && isMusicPrepared) {
            mediaPlayer?.start()
        }
    }

    @Synchronized
    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping music", e)
        }
        mediaPlayer = null
        isMusicPrepared = false
        Log.d(TAG, "Background music stopped")
    }

    // Volume configuration (تؤثر على الموسيقى والمؤثرات)
    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        // ضبط مستوى صوت الموسيقى إذا كان mediaPlayer موجوداً
        mediaPlayer?.setVolume(musicVolume, musicVolume)
        // إذا كان مستوى الصوت صفراً والموسيقى مشتغلة، أوقفها
        if (musicVolume <= 0f && mediaPlayer?.isPlaying == true) {
            stopMusic()
        } else if (musicVolume > 0f && mediaPlayer?.isPlaying == false && isMusicPrepared) {
            startMusic()
        }
    }
}