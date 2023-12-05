package net.idrnd.idvoicegpt.speechrecognition

import androidx.lifecycle.LiveData

/**
 * Uses an SpeechService to recognize voice bytes and exposes recognized text through a livedata property.
 */
interface SpeechServiceManager {

    // Contains recognized text.
    val recognizedText: LiveData<Response>

    /**
     * Wraps @see SpeechService.startRecognizing.
     */
    fun startRecognizing(sampleRate: Int)

    /**
     * Wraps @see SpeechService.recognize.
     */
    fun recognize(bytes: ByteArray, size: Int)

    /**
     * Wraps @see SpeechService.finishRecognizing.
     */
    fun finishRecognizing()

    /**
     * Setup SpeechService ready to use for recognition.
     *
     * @param speechService The bound SpeechService.
     */
    fun connectService(speechService: SpeechService)

    /**
     * Breakdowns the service.
     */
    fun disconnectService()

    sealed class Response {
        data class RecognizedText(val text: String, val isFinal: Boolean) : Response()
        data class Error(val message: String) : Response()
    }
}
