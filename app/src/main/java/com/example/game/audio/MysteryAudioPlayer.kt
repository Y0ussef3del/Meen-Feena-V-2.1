package com.example.game.audio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sin
import kotlin.random.Random

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    @Volatile
    private var musicVolume = 0.5f

    private val resourceCountCache = ConcurrentHashMap<String, Int>()
    private val unusedAudioPools = ConcurrentHashMap<String, MutableList<Int>>()

    fun shutdown() {
        scope.coroutineContext.cancelChildren()
    }

    fun playClick(context: Context) {
        playRawResource(context, "click")
    }

    fun playSelection() {
        scope.launch {
            try {
                val sampleRate = 22050
                val numSamples = (sampleRate * 0.04).toInt()
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

    fun playSuccess() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.22
                val numSamples = (sampleRate * duration).toInt()
                val buffer = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val freq = when {
                        t < 0.07 -> 523.25
                        t < 0.14 -> 659.25
                        else -> 783.99
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

    fun playError() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.25
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

    fun playWarning() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.15
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

    fun playVote() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.12
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

    fun playTransition() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.45
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

    fun playTension() {
        playReveal()
    }

    fun playReveal() {
        scope.launch {
            try {
                val sampleRate = 22050
                val duration = 0.8
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

    @Suppress("DEPRECATION")
    private suspend fun playBufferStatic(buffer: ShortArray, sampleRate: Int) {
        val bufferSizeInBytes = buffer.size * 2

        val audioTrack = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val format = AudioFormat.Builder()
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setSampleRate(sampleRate)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()
            AudioTrack.Builder()
                .setAudioAttributes(attributes)
                .setAudioFormat(format)
                .setBufferSizeInBytes(bufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSizeInBytes,
                AudioTrack.MODE_STATIC
            )
        }

        try {
            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.notificationMarkerPosition = buffer.size

            val playbackCompletion = CompletableDeferred<Unit>()

            audioTrack.setPlaybackPositionUpdateListener(object : AudioTrack.OnPlaybackPositionUpdateListener {
                override fun onMarkerReached(track: AudioTrack?) {
                    playbackCompletion.complete(Unit)
                }
                override fun onPeriodicNotification(track: AudioTrack?) {}
            })

            audioTrack.play()

            val estimatedDurationMs = ((buffer.size.toDouble() / sampleRate) * 1000).toLong() + 200L
            withTimeoutOrNull(estimatedDurationMs) {
                playbackCompletion.await()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during static playback", e)
        } finally {
            try {
                if (audioTrack.state != AudioTrack.STATE_UNINITIALIZED) {
                    audioTrack.stop()
                    audioTrack.release()
                }
            } catch (_: Throwable) {}
        }
    }

    @SuppressLint("DiscouragedApi")
    private fun countAvailableResources(context: Context, prefix: String): Int {
        return resourceCountCache.getOrPut(prefix) {
            var count = 0
            val appContext = context.applicationContext
            while (true) {
                val nextIndex = count + 1
                val resId = appContext.resources.getIdentifier("${prefix}_$nextIndex", "raw", appContext.packageName)
                if (resId != 0) {
                    count = nextIndex
                } else {
                    break
                }
            }
            count
        }
    }

    private fun getNextAudioIndex(context: Context, prefix: String): Int {
        val totalCount = countAvailableResources(context, prefix)
        if (totalCount <= 0) return 1

        synchronized(unusedAudioPools) {
            val pool = unusedAudioPools.getOrPut(prefix) { mutableListOf() }
            if (pool.isEmpty()) {
                pool.addAll(1..totalCount)
            }
            val randomIndex = Random.nextInt(pool.size)
            return pool.removeAt(randomIndex)
        }
    }

    fun playPlayerEliminatedInnocent(context: Context) {
        val index = getNextAudioIndex(context, "innocent")
        playRawResource(context, "innocent_$index")
    }

    fun playPlayerEliminatedCriminal(context: Context) {
        val index = getNextAudioIndex(context, "criminal")
        playRawResource(context, "criminal_$index")
    }

    fun playGameOverSound(context: Context, isWin: Boolean) {
        val prefix = if (isWin) "game_win" else "game_lose"
        val index = getNextAudioIndex(context, prefix)
        playRawResource(context, "${prefix}_$index")
    }

    @SuppressLint("DiscouragedApi")
    private fun playRawResource(context: Context, resName: String) {
        val appContext = context.applicationContext
        scope.launch(Dispatchers.IO) {
            try {
                val resId = appContext.resources.getIdentifier(resName, "raw", appContext.packageName)
                if (resId != 0) {
                    val mediaPlayer = MediaPlayer.create(appContext, resId)
                    mediaPlayer?.apply {
                        setVolume(musicVolume, musicVolume)
                        setOnCompletionListener { mp ->
                            try { mp.release() } catch (_: Exception) {}
                        }
                        setOnErrorListener { mp, _, _ ->
                            try { mp.release() } catch (_: Exception) {}
                            true
                        }
                        start()
                    }
                } else {
                    Log.e(TAG, "Resource $resName not found in res/raw")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing raw resource: $resName", e)
            }
        }
    }

    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
    }
}