package net.idrnd.idvoicegpt.speaker

import java.util.Locale

/**
 * An abstraction of a voice synthesizer that can speak.
 */
interface Speaker {

    /**
     * Speaks provided text with locale if available, otherwise use default.
     * @param text The text to speak.
     */
    fun speak(text: String, locale: Locale?)

    /**
     * Tells if it´s currently speaking or not
     * @return a boolean telling if it´s speaking or not.
     */
    fun isSpeaking(): Boolean

    /**
     * Stops speaking.
     */
    fun stopSpeak()
}
