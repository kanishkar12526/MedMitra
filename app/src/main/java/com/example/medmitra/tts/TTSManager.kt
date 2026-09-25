package com.example.medmitra.tts

import android.content.Context
import android.media.AudioAttributes
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.medmitra.data.local.UserPreferencesManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.UUID

fun getMultilingualAnnouncement(lang: String, medicationName: String): String {
    val template = when (lang) {
        "hi" -> "स्वस्थ रहने के लिए अपनी दवा %s लें"
        "ta" -> "வணக்கம்! உங்கள் உடல்நலத்தைப் பேண %s மருந்தை இப்போது உட்கொள்ளுங்கள்"
        "te" -> "ఆరోగ్యంగా ఉండటానికి మీ మందు %s తీసుకోండి"
        "kn" -> "ಆರೋಗ್ಯವಾಗಿರಲು ನಿಮ್ಮ ಔಷಧ %s ತೆಗೆದುಕೊಳ್ಳಿ"
        "ml" -> "ആരോഗ്യത്തോടെയിരിക്കാൻ നിങ്ങളുടെ മരുന്ന് %s കഴിക്കുക"
        "mr" -> "निरोगी राहण्यासाठी तुमचे औषध %s घ्या"
        "gu" -> "તંદુરસ્ત રહેવા માટે તમારી દવા %s લો"
        "bn" -> "সুস্থ থাকতে আপনার ওষুধ %s নিন"
        else -> "Hello! Please take your medicine %s now to stay healthy."
    }
    return String.format(template, medicationName)
}

class TTSManager(
    private val context: Context,
    private val languageCode: String? = null
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private val initDeferred = CompletableDeferred<Boolean>()
    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Main)

    init {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            initTts()
        } else {
            Handler(Looper.getMainLooper()).post {
                initTts()
            }
        }
    }

    private fun initTts() {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("TTSManager", "Failed to instantiate TextToSpeech", e)
            isInitialized = false
            initDeferred.complete(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            try {
                val userLang = languageCode ?: UserPreferencesManager.getInstance(context).appLanguage
                val targetLocale = Locale.forLanguageTag(userLang)
                var result = tts?.setLanguage(targetLocale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts?.setLanguage(Locale.getDefault())
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        tts?.setLanguage(Locale.US)
                    }
                }

                // High Quality Speech Audio Tuning
                tts?.setSpeechRate(0.88f) // Clear, enunciated speech cadence for senior clarity
                tts?.setPitch(1.02f)      // Crisp, resonant vocal tone

                // Select Highest Quality/HQ Voice if available
                try {
                    val voices = tts?.voices
                    val hqVoice = voices?.firstOrNull { voice ->
                        voice.locale.language == targetLocale.language &&
                                !voice.isNetworkConnectionRequired &&
                                (voice.quality >= 400 || voice.name.contains("hq", ignoreCase = true) || voice.name.contains("high", ignoreCase = true))
                    } ?: voices?.firstOrNull { voice -> voice.locale.language == targetLocale.language }
                    
                    if (hqVoice != null) {
                        tts?.voice = hqVoice
                        Log.i("TTSManager", "HQ Voice selected: ${hqVoice.name}")
                    }
                } catch (e: Throwable) {
                    Log.w("TTSManager", "Error selecting HQ voice", e)
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                isInitialized = true
                initDeferred.complete(true)
            } catch (e: Exception) {
                Log.e("TTSManager", "Error setting language or attributes in TTS", e)
                isInitialized = true
                initDeferred.complete(true)
            }
        } else {
            Log.e("TTSManager", "TTS initialization failed with status $status")
            isInitialized = false
            initDeferred.complete(false)
        }
    }

    suspend fun speakAndWait(text: String, timeoutMillis: Long = 10000L) {
        withContext(Dispatchers.Main) {
            val initialized = withTimeoutOrNull(4000L) {
                initDeferred.await()
            } ?: false

            if (!initialized || tts == null) {
                shutdown()
                return@withContext
            }

            val utteranceId = UUID.randomUUID().toString()
            val speakDeferred = CompletableDeferred<Unit>()

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceIdParam: String?) {}

                override fun onDone(utteranceIdParam: String?) {
                    if (utteranceIdParam == utteranceId) {
                        speakDeferred.complete(Unit)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceIdParam: String?) {
                    if (utteranceIdParam == utteranceId) {
                        speakDeferred.complete(Unit)
                    }
                }

                override fun onError(utteranceIdParam: String?, errorCode: Int) {
                    if (utteranceIdParam == utteranceId) {
                        speakDeferred.complete(Unit)
                    }
                }
            })

            val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
            if (result == TextToSpeech.SUCCESS) {
                withTimeoutOrNull(timeoutMillis) {
                    speakDeferred.await()
                }
            }
            shutdown()
        }
    }

    fun speak(text: String) {
        scope.launch {
            val initialized = withTimeoutOrNull(3000L) {
                initDeferred.await()
            } ?: false

            if (initialized && tts != null) {
                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
            }
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
