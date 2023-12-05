package net.idrnd.idvoicegpt.speechrecognition

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Concretion of SpeechServiceManager that uses SpeechService for speech to text.
 */
class DefaultSpeechServiceManager : SpeechServiceManager {

    // The service to use for speech recognition.
    private var service: SpeechService? = null

    private val _recognizedText = MutableLiveData<SpeechServiceManager.Response>()
    override val recognizedText: LiveData<SpeechServiceManager.Response>
        get() = _recognizedText

    private val speechRecognitionListener: SpeechRecognitionListener = object :
        SpeechRecognitionListener {
        override fun onSpeechRecognized(text: String, isFinal: Boolean) {
            _recognizedText.postValue(SpeechServiceManager.Response.RecognizedText(text, isFinal))
        }
    }

    override fun startRecognizing(sampleRate: Int) {
        service?.startRecognizing(sampleRate)
    }

    override fun recognize(bytes: ByteArray, size: Int) {
        service?.recognize(bytes, size)
    }

    override fun connectService(speechService: SpeechService) {
        service = speechService
        service?.addListener(speechRecognitionListener)
    }

    override fun disconnectService() {
        service?.removeListener(speechRecognitionListener)
        service = null
    }

    override fun finishRecognizing() {
        service?.finishRecognizing()
    }
}
