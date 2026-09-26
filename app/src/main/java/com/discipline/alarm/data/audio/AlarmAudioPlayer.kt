package com.discipline.alarm.data.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes as Media3AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmAudioPlayer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "AlarmAudioPlayer"
        private val VIBRATION_PATTERN = longArrayOf(0, 800, 400, 800, 400, 1200)
    }

    private var exoPlayer: ExoPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
        vibratorManager?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    private var fadeInJob: Job? = null

    fun isPlaying(): Boolean {
        return try {
            exoPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    @OptIn(UnstableApi::class)
    fun playAlarm(soundUriString: String?, targetVolume: Float = 1.0f, enableVibration: Boolean = true) {
        if (isPlaying()) {
            Log.d(TAG, "Alarm audio is already playing continuously in background; keeping current playback.")
            if (enableVibration) {
                startVibration()
            }
            return
        }
        stopAlarm()

        requestAudioFocus()

        val soundUri = resolveSoundUri(soundUriString)
        Log.d(TAG, "Playing alarm sound: $soundUri, targetVolume=$targetVolume")

        try {
            val media3AudioAttributes = Media3AudioAttributes.Builder()
                .setUsage(C.USAGE_ALARM)
                .setContentType(C.AUDIO_CONTENT_TYPE_SONIFICATION)
                .build()

            exoPlayer = ExoPlayer.Builder(context).build().apply {
                setAudioAttributes(media3AudioAttributes, false)
                repeatMode = Player.REPEAT_MODE_ONE
                volume = 0.15f // Initial soft volume for progressive fade-in

                val mediaItem = MediaItem.fromUri(soundUri)
                setMediaItem(mediaItem)
                prepare()
                play()
            }

            // Start progressive volume fade-in over 4 seconds
            fadeInJob = CoroutineScope(Dispatchers.Main).launch {
                val steps = 10
                val volumeIncrement = (targetVolume - 0.15f) / steps
                var currentVolume = 0.15f

                for (i in 1..steps) {
                    delay(400L)
                    currentVolume += volumeIncrement
                    exoPlayer?.volume = currentVolume.coerceIn(0f, 1f)
                }
            }

            if (enableVibration) {
                startVibration()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error playing alarm audio", e)
        }
    }

    fun stopAlarm() {
        Log.d(TAG, "Stopping alarm audio and vibration")
        fadeInJob?.cancel()
        fadeInJob = null

        try {
            exoPlayer?.stop()
            exoPlayer?.release()
            exoPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ExoPlayer", e)
        }

        stopVibration()
        abandonAudioFocus()
    }

    private fun resolveSoundUri(soundUriString: String?): Uri {
        if (!soundUriString.isNullOrBlank()) {
            try {
                if (soundUriString.equals("kafir", ignoreCase = true) ||
                    soundUriString.equals("kafir.mp3", ignoreCase = true) ||
                    soundUriString.contains("raw/kafir")
                ) {
                    val resId = context.resources.getIdentifier("kafir", "raw", context.packageName)
                    if (resId != 0) {
                        return Uri.parse("android.resource://${context.packageName}/$resId")
                    }
                }
                val uri = Uri.parse(soundUriString)
                if (uri != null) return uri
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse soundUri: $soundUriString, falling back to default", e)
            }
        }

        // If kafir sound exists in raw resources, use as primary discipline alarm sound
        val defaultRawId = context.resources.getIdentifier("kafir", "raw", context.packageName)
        if (defaultRawId != 0) {
            return Uri.parse("android.resource://${context.packageName}/$defaultRawId")
        }

        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Uri.EMPTY
    }

    private fun startVibration() {
        vibrator?.let { vib ->
            if (vib.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(VIBRATION_PATTERN, 0) // repeat at index 0
                    vib.vibrate(effect)
                } else {
                    @Suppress("DEPRECATION")
                    vib.vibrate(VIBRATION_PATTERN, 0)
                }
            }
        }
    }

    private fun stopVibration() {
        vibrator?.cancel()
    }

    private fun requestAudioFocus() {
        if (audioManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(false)
                .setOnAudioFocusChangeListener { focusChange ->
                    Log.d(TAG, "AudioFocus changed: $focusChange")
                }
                .build()

            audioManager.requestAudioFocus(audioFocusRequest!!)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                null,
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE
            )
        }
    }

    private fun abandonAudioFocus() {
        if (audioManager == null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let {
                audioManager.abandonAudioFocusRequest(it)
                audioFocusRequest = null
            }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
