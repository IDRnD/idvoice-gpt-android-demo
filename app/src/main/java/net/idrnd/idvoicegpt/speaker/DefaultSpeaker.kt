package net.idrnd.idvoicegpt.speaker

import android.speech.tts.TextToSpeech
import java.util.Locale

/**
 * Uses android TextToSpeech to synthesize a voice to read a text
 */
class DefaultSpeaker(
    private val textToSpeech: TextToSpeech
) : Speaker {

    /**
     * Read provided text.
     *
     * @param text The text to read.
     * @locale optional locale to use, using the one TextToSpeech property has defined
     */
    override fun speak(text: String, locale: Locale?) {
        if (locale != null) {
            textToSpeech.language = locale
        }
        if (text.isNotEmpty()) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "tts1")
        }
    }

    /**
     * Tells whether it is currently speaking or not
     *
     * @return true if speaking is in process else false
     */
    override fun isSpeaking(): Boolean = textToSpeech.isSpeaking

    /**
     * Stops the speaking.
     */
    override fun stopSpeak() {
        textToSpeech.stop()
    }
}
