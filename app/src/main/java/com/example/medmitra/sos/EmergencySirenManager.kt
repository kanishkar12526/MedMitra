package com.example.medmitra.sos

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.ToneGenerator
import android.util.Log
import com.example.medmitra.data.local.UserPreferencesManager
import kotlin.math.sin

class EmergencySirenManager(private val context: Context) {

    private var audioTrack: AudioTrack? = null
    private var toneGenerator: ToneGenerator? = null
    private var isPlaying = false

    companion object {
        private const val TAG = "EmergencySirenManager"
        private const val SAMPLE_RATE = 44100
        private const val DURATION_SECONDS = 2
    }

    @Synchronized
    fun startSiren() {
        val userPrefs = UserPreferencesManager.getInstance(context)
        if (!userPrefs.isSosSirenEnabled) {
            Log.i(TAG, "Emergency siren is disabled in user preferences")
            return
        }
        if (isPlaying) return
        isPlaying = true

        try {
            // Set alarm volume to maximum on STREAM_ALARM
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.let { am ->
                val maxVol = am.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                am.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            }

            // Generate high-frequency wailing siren sound PCM buffer (1500 Hz to 3000 Hz sweep)
            val numSamples = SAMPLE_RATE * DURATION_SECONDS
            val pcmData = ShortArray(numSamples)

            for (i in 0 until numSamples) {
                val t = i.toDouble() / SAMPLE_RATE
                // 2 Hz sweep frequency modulating between 1500 Hz and 3000 Hz
                val frequency = 1500.0 + 1500.0 * (0.5 + 0.5 * sin(2.0 * Math.PI * 2.0 * t))
                val phase = 2.0 * Math.PI * frequency * t
                val sampleValue = (sin(phase) * Short.MAX_VALUE * 0.95).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                pcmData[i] = sampleValue.toShort()
            }

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val audioFormat = AudioFormat.Builder()
                .setSampleRate(SAMPLE_RATE)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .build()

            val bufferSize = pcmData.size * 2 // 2 bytes per 16-bit sample

            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(audioAttributes)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build().apply {
                    write(pcmData, 0, pcmData.size)
                    setLoopPoints(0, numSamples, -1) // Loop continuously
                    play()
                }

            Log.i(TAG, "Emergency siren started successfully at MAX volume on STREAM_ALARM")
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack siren initialization failed, falling back to ToneGenerator", e)
            try {
                toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME).apply {
                    startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 30000)
                }
            } catch (t: Throwable) {
                Log.e(TAG, "ToneGenerator fallback failed", t)
            }
        }
    }

    @Synchronized
    fun stopSiren() {
        if (!isPlaying) return
        isPlaying = false

        try {
            audioTrack?.apply {
                if (playState == AudioTrack.PLAYSTATE_PLAYING) {
                    stop()
                }
                release()
            }
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping AudioTrack", e)
        }

        try {
            toneGenerator?.apply {
                stopTone()
                release()
            }
            toneGenerator = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping ToneGenerator", e)
        }

        Log.i(TAG, "Emergency siren stopped")
    }
}
