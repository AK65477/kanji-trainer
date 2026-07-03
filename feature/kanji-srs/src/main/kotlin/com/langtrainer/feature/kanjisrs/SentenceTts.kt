package com.langtrainer.feature.kanjisrs

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Thin wrapper around a Japanese [TextToSpeech] engine for reading example
 * sentences aloud. [isReady] is false until the engine initialises and a Japanese
 * voice is available, so the UI can hide the speak button when playback is not
 * possible (e.g. no Japanese TTS data installed).
 */
class SentenceTts(
    private val engine: TextToSpeech?,
    val isReady: Boolean,
) {
    fun speak(text: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "sentence")
    }
}

/**
 * Creates a screen-scoped Japanese TTS engine, initialised once and shut down when
 * the composable leaves the composition. The engine outlives individual cards.
 */
@Composable
fun rememberSentenceTts(): SentenceTts {
    val context = LocalContext.current
    var ready by remember { mutableStateOf(false) }
    val engineHolder = remember { mutableStateOf<TextToSpeech?>(null) }

    DisposableEffect(Unit) {
        val engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = engineHolder.value?.setLanguage(Locale.JAPANESE)
                ready = result == TextToSpeech.LANG_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_AVAILABLE ||
                    result == TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE
            }
        }
        engineHolder.value = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            engineHolder.value = null
        }
    }

    return SentenceTts(engineHolder.value, ready)
}
