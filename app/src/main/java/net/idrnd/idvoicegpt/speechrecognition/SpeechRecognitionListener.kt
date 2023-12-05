package net.idrnd.idvoicegpt.speechrecognition

/**
 * A listener for voice recognition transcriptions.
 */
interface SpeechRecognitionListener {
    /**
     * Called when a new piece of text was recognized by the Speech API.
     *
     * @param text The text.
     * @param isFinal `true` when the API finished processing audio.
     */
    fun onSpeechRecognized(text: String, isFinal: Boolean)
}
