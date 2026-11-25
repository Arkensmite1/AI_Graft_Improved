package com.aigraft.farmer.audio

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.aigraft.farmer.i18n.AppLang
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

private const val TAG = "SpeechManager"

enum class SpeechStatus { IDLE, LOADING, PLAYING, ERROR }

data class SpeechState(
    val activeId: String? = null,
    val status: SpeechStatus = SpeechStatus.IDLE,
)

class SpeechManager(context: Context) {
    private val _state = MutableStateFlow(SpeechState())
    val state: StateFlow<SpeechState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingRequest: Triple<String, String, AppLang>? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        if (_state.value.activeId == utteranceId) {
                            _state.value = SpeechState(utteranceId, SpeechStatus.PLAYING)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        if (_state.value.activeId == utteranceId) {
                            _state.value = SpeechState()
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        if (_state.value.activeId == utteranceId) {
                            _state.value = SpeechState(utteranceId, SpeechStatus.ERROR)
                        }
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        Log.e(TAG, "TTS error code=$errorCode for $utteranceId")
                        if (_state.value.activeId == utteranceId) {
                            _state.value = SpeechState(utteranceId, SpeechStatus.ERROR)
                        }
                    }
                })
                pendingRequest?.let { (id, text, lang) ->
                    pendingRequest = null
                    speakInternal(id, text, lang)
                }
            } else {
                Log.e(TAG, "TTS initialization failed status=$status")
                pendingRequest?.let { (id, _, _) ->
                    _state.value = SpeechState(id, SpeechStatus.ERROR)
                    pendingRequest = null
                }
            }
        }
    }

    fun speak(id: String, rawText: String, lang: AppLang) {
        val text = cleanText(rawText)
        if (text.isBlank()) return

        _state.value = SpeechState(id, SpeechStatus.LOADING)
        if (!ready) {
            pendingRequest = Triple(id, text, lang)
            return
        }
        speakInternal(id, text, lang)
    }

    private fun speakInternal(id: String, text: String, lang: AppLang) {
        val engine = tts
        if (engine == null) {
            _state.value = SpeechState(id, SpeechStatus.ERROR)
            return
        }
        val locale = if (lang == AppLang.HI) Locale("hi", "IN") else Locale("en", "IN")
        val langResult = engine.setLanguage(locale)
        if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Fall back to default English if Hindi voice data isn't installed on emulator
            engine.setLanguage(Locale.US)
        }
        engine.setSpeechRate(0.86f)
        engine.setPitch(1.0f)
        val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
        if (res == TextToSpeech.ERROR) {
            _state.value = SpeechState(id, SpeechStatus.ERROR)
        }
    }

    fun stop() {
        pendingRequest = null
        runCatching { tts?.stop() }
        _state.value = SpeechState()
    }

    fun shutdown() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
        ready = false
    }

    private fun cleanText(raw: String): String =
        raw.replace(Regex("https?://\\S+"), "")
            .replace(Regex("[*_#>~|`]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
}
