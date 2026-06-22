package com.example.game.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import kotlinx.coroutines.*

object MysteryAudioPlayer {
    private const val TAG = "MysteryAudioPlayer"
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var musicVolume = 0.5f
    private var isVolumeLowered = false // مؤشر للحفاظ على خفض الصوت عبر الشاشات
    private var mediaPlayer: MediaPlayer? = null
    private var sfxPlayer: MediaPlayer? = null

    // تشغيل صوت الأزرار المميز من ملف mp3
    fun playClick(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val resId = context.resources.getIdentifier("button_click", "raw", context.packageName)
                if (resId != 0) {
                    MediaPlayer.create(context, resId).apply {
                        // تشغيل المؤثر الصوتي بمستوى الصوت المعتمد
                        val vol = if (isVolumeLowered) musicVolume * 0.4f else musicVolume
                        setVolume(vol, vol)
                        setOnCompletionListener { release() }
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing button click sound", e)
            }
        }
    }

    // متوافق مع الاستدعاء القديم بدون سياق لعدم كسر أي كود فرعي آخر
    fun playClick() {
        // احتياطي للتوافقية
    }

    fun playSelection() {
        // احتياطي للتوافقية
    }

    // صوت مميز عند عرض الدلائل لجميع الأجهزة
    fun playEvidenceReveal(context: Context) {
        scope.launch(Dispatchers.IO) {
            try {
                val resId = context.resources.getIdentifier("evidence_reveal", "raw", context.packageName)
                if (resId != 0) {
                    MediaPlayer.create(context, resId).apply {
                        val vol = if (isVolumeLowered) musicVolume * 0.4f else musicVolume
                        setVolume(vol, vol)
                        setOnCompletionListener { release() }
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing evidence reveal sound", e)
            }
        }
    }

    // تعديل مستوى الصوت الأساسي
    fun setVolume(volume: Float) {
        musicVolume = volume.coerceIn(0.0f, 1.0f)
        applyCurrentVolume()
    }

    // تطبيق مستوى الصوت الحالي بناءً على حالة اللعبة (منخفض أم طبيعي)
    private fun applyCurrentVolume() {
        try {
            val targetVol = if (isVolumeLowered) (musicVolume * 0.3f) else musicVolume
            mediaPlayer?.setVolume(targetVol, targetVol)
        } catch (e: Exception) {
            Log.e(TAG, "Error applying volume changes", e)
        }
    }

    // توطية الموسيقى تلقائياً عند بدء شاشة المناقشة وتثبيتها
    fun lowerVolumeForDiscussion() {
        isVolumeLowered = true
        applyCurrentVolume()
        Log.d(TAG, "Volume lowered and locked for active gameplay phases.")
    }

    // إعادة الصوت لطبيعته (يتم استدعاؤها فقط عند العودة للقائمة الرئيسية أو بدء جولة جديدة تماماً)
    fun restoreNormalVolume() {
        isVolumeLowered = false
        applyCurrentVolume()
        Log.d(TAG, "Normal volume restored.")
    }

    // تشغيل الخلفية الموسيقية تكرارياً من مجلد raw
    fun startMusic(context: Context) {
        if (mediaPlayer == null) {
            try {
                val resId = context.resources.getIdentifier("music_background", "raw", context.packageName)
                if (resId != 0) {
                    mediaPlayer = MediaPlayer.create(context, resId).apply {
                        isLooping = true
                        start()
                    }
                    applyCurrentVolume() // التأكد من تطبيق خفض الصوت إذا كان مفعلاً مسبقاً
                } else {
                    Log.e(TAG, "Resource music_background not found in res/raw")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting background music", e)
            }
        } else {
            if (!mediaPlayer!!.isPlaying) {
                mediaPlayer?.start()
            }
        }
    }

    fun stopMusic() {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping background music", e)
        }
    }

    // موسيقى مخصصة عند خروج لاعب (بريء أو مجرم) - تحافظ على انخفاض الخلفية الموسيقية
    fun playEliminationResultMusic(context: Context, isMafia: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                sfxPlayer?.stop()
                sfxPlayer?.release()

                val fileName = if (isMafia) "mafia_eliminated" else "innocent_eliminated"
                val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
                if (resId != 0) {
                    sfxPlayer = MediaPlayer.create(context, resId).apply {
                        setVolume(musicVolume, musicVolume)
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing elimination music", e)
            }
        }
    }

    // موسيقى مخصصة للشاشة النهائية (عندها يتم إلغاء قفل خفض الصوت لإعادة تهيئة اللعبة لاحقاً)
    fun playEndgameResultMusic(context: Context, isInnocentsWinner: Boolean) {
        scope.launch(Dispatchers.IO) {
            try {
                stopMusic() // إيقاف موسيقى الخلفية المعتادة لعرض موسيقى النهاية المناسبة بوضوح
                isVolumeLowered = false // إلغاء القفل للاستعداد للجولة القادمة عند الخروج
                
                sfxPlayer?.stop()
                sfxPlayer?.release()

                val fileName = if (isInnocentsWinner) "innocents_win" else "mafia_win"
                val resId = context.resources.getIdentifier(fileName, "raw", context.packageName)
                if (resId != 0) {
                    sfxPlayer = MediaPlayer.create(context, resId).apply {
                        isLooping = false
                        setVolume(musicVolume, musicVolume)
                        start()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error playing endgame music", e)
            }
        }
    }
}