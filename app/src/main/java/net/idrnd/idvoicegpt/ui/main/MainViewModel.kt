package net.idrnd.idvoicegpt.ui.main

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.preference.PreferenceManager
import com.aallam.openai.api.core.FinishReason
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.idrnd.IDVoiceManager
import net.idrnd.idvoicegpt.idrnd.IDVoiceResponse
import net.idrnd.idvoicegpt.questionsender.QuestionSender
import net.idrnd.idvoicegpt.speaker.Speaker
import net.idrnd.idvoicegpt.speechrecognition.SpeechService
import net.idrnd.idvoicegpt.speechrecognition.SpeechServiceManager
import net.idrnd.idvoicegpt.ui.ChatIntent
import net.idrnd.idvoicegpt.ui.ChatMessages
import net.idrnd.idvoicegpt.ui.Error
import net.idrnd.idvoicegpt.ui.UIState
import net.idrnd.idvoicegpt.util.IDVoiceMessageGetter
import net.idrnd.idvoicegpt.util.getAppPreferences
import net.idrnd.idvoicegpt.util.removeHistoryJson
import net.idrnd.idvoicegpt.util.saveHistoryJson
import net.idrnd.idvoicegpt.voice.VoiceRecorderManager
import java.util.Locale

/**
 * ViewModel for main screen.
 */
class MainViewModel(
    context: Context,
    private val idrndVoiceManager: IDVoiceManager,
    private val speechServiceManager: SpeechServiceManager,
    private val voiceRecorderManager: VoiceRecorderManager,
    private val questionSender: QuestionSender,
    private val speaker: Speaker
) : ViewModel() {

    private val settingsSharedPreferences: SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context)
    private val appSharedPreferences: SharedPreferences = context.getAppPreferences()

    private var isConnected: Boolean = true
    private var listened = ""
    private var listenedList = mutableListOf<String>()
    private var question = ""
    private var response = ""
    private var firstQuestion = true

    val userIntent = Channel<ChatIntent>(Channel.UNLIMITED)
    private val _mainState =
        MutableStateFlow<UIState>(
            UIState.Idle(
                firstQuestion,
                question,
                response,
                listened,
                isConnected
            )
        )
    val mainState: StateFlow<UIState>
        get() = _mainState

    private val _errorChannel = Channel<Error>()
    val errorFlow = _errorChannel.receiveAsFlow()

    private val history = mutableListOf<ChatMessages>()

    /**
     * Receives recognized text and update state with it, this gives the dynamic effect in edit text.
     */
    private val recognizedTextObserver = Observer<SpeechServiceManager.Response> {
        when {
            _mainState.value is UIState.Listening && it is SpeechServiceManager.Response.RecognizedText -> {
                listened = "${listenedList.joinToString()} ${it.text}"
                _mainState.value = UIState.Listening(listened, question, response, firstQuestion)
                if (it.isFinal) {
                    listenedList.add(it.text)
                }
            }

            it is SpeechServiceManager.Response.Error -> {
                Log.w(TAG, it.message)
                viewModelScope.launch {
                    _errorChannel.send(Error.RecognitionError)
                    _mainState.value =
                        UIState.Listening(listened, question, response, firstQuestion)
                }
            }
        }
    }

    /**
     * Bytes from voice recording, IDVoiceSDK and Google Speech Recognition API use these bytes
     */
    private var recordingByteArray = byteArrayOf()
    private val sampleRate = voiceRecorderManager.getSampleRate()

    private var sendQuestionJob: Job? = null
    private var listenJob: Job? = null

    private val preferenceLanguageKey = context.getString(R.string.preference_language_key)

    private lateinit var idVoiceMessageGetter: IDVoiceMessageGetter

    init {
        speechServiceManager.recognizedText.observeForever(recognizedTextObserver)
        setUserIntentsHandler()
    }

    private fun setUserIntentsHandler() {
        viewModelScope.launch {
            userIntent.consumeAsFlow().collect {
                when (it) {
                    is ChatIntent.CleanUpAppIntent -> reset()
                    is ChatIntent.ListenIntent -> listen()
                    is ChatIntent.SendQuestionIntent -> sendQuestion()
                    is ChatIntent.CancelListeningIntent -> cancelListening()
                    is ChatIntent.StopAnsweringIntent -> stopAnswering(it.partialText)
                    is ChatIntent.Speak -> speak()
                    is ChatIntent.ConnectionUpdateIntent -> handleNetworkUpdate(it.isConnected)
                }
            }
        }
    }

    private fun handleNetworkUpdate(isConnected: Boolean) {
        if (this.isConnected == isConnected) {
            return
        }
        this.isConnected = isConnected

        if (!isConnected) {
            when (_mainState.value) {
                is UIState.Listening -> cancelListening()
                is UIState.Answering, is UIState.Thinking -> stopAnswering(response)
                else -> { /* No action needed */ }
            }
        }
        _mainState.value = UIState.Idle(firstQuestion, question, response, listened, isConnected)
    }

    private fun speak() {
        when (speaker.isSpeaking()) {
            true -> speaker.stopSpeak()
            else -> speaker.speak(
                response,
                settingsSharedPreferences.getString(preferenceLanguageKey, Locale.US.toLanguageTag())?.let {
                    Locale.forLanguageTag(it)
                }
            )
        }
    }

    private fun reset() {
        listened = ""
        listenedList.clear()
        question = ""
        response = ""
        firstQuestion = true
        resetRecording()
        idrndVoiceManager.reset()
        history.clear()
        appSharedPreferences.removeHistoryJson()
        _mainState.value = UIState.Idle(true, question, response, listened, isConnected)
    }

    private fun stopAnswering(partialText: String) {
        viewModelScope.launch {
            sendQuestionJob?.cancel()
            response = partialText
            _mainState.value = UIState.Idle(false, question, response, listened, isConnected)
        }
    }

    private fun cancelListening() {
        resetRecording()
        stopListeningVoiceAndSpeech()
        listenJob?.cancel()
        _mainState.value = UIState.Idle(firstQuestion, question, response, listened, isConnected)
        listened = ""
        listenedList.clear()
    }

    fun stopListeningVoiceAndSpeech() {
        viewModelScope.launch(Dispatchers.IO) {
            voiceRecorderManager.stop()
            speechServiceManager.finishRecognizing()
            listenJob?.cancel()
        }
    }

    override fun onCleared() {
        stopListeningVoiceAndSpeech()
        super.onCleared()
    }

    private fun listen() {
        listenJob = viewModelScope.launch(Dispatchers.IO) {
            listened = ""
            listenedList.clear()
            _mainState.value = UIState.Listening(listened, question, response, firstQuestion)
            startRecording()
        }
    }

    /**
     * Accumulates recorded bytes that IDVoiceSDk will use to perform verifications
     * Also send recorded bytes speech recognition manager to get transcriptions
     */
    private suspend fun startRecording() {
        voiceRecorderManager.startRecording().collect {
            when (it) {
                is VoiceRecorderManager.Response.VoiceStart -> {
                    speechServiceManager.startRecognizing(sampleRate)
                }

                is VoiceRecorderManager.Response.Voice -> {
                    recordingByteArray += it.bytes
                    speechServiceManager.recognize(it.bytes, it.chunkSize)
                }

                is VoiceRecorderManager.Response.VoiceEnd -> {
                    speechServiceManager.finishRecognizing()
                }

                is VoiceRecorderManager.Response.Error -> {
                    speechServiceManager.finishRecognizing()
                    Log.w(TAG, it.message)
                    _errorChannel.send(Error.RecordingError)
                    _mainState.value = UIState.Listening(listened, question, response, firstQuestion)
                }
            }
        }
    }

    /**
     * Sends question to ChatGPT if speaker verification succeeded
     * otherwise shows verification failed message to user
     */
    private suspend fun sendQuestion() {
        question = listened
        Log.w(TAG, "Question: $question")
        _mainState.value = UIState.Thinking(question)
        sendQuestionJob = viewModelScope.launch(Dispatchers.IO) {
            stopListeningVoiceAndSpeech()

            listened = ""
            listenedList.clear()
            response = ""
            firstQuestion = false

            if (recordingByteArray.isEmpty()) {
                Log.w(TAG, "Empty input audio buffer")
                _errorChannel.send(Error.SendQuestionError)
                _mainState.value = UIState.Idle(firstQuestion, question, response, listened, isConnected)
                return@launch
            }

            // Usage of IDVoiceSDK to perform the verification.
            val response =
                idrndVoiceManager.createAndVerifyTemplate(recordingByteArray.copyOf(), sampleRate)

            // Clear it so it don´t get mixed bytes on future data.
            resetRecording()

            if (response is IDVoiceResponse.Failure) {
                this@MainViewModel.response = idVoiceMessageGetter.getFailureMessage(response)
                saveHistory()
                _mainState.value = UIState.Idle(
                    firstQuestion,
                    question,
                    this@MainViewModel.response,
                    listened,
                    isConnected
                )
                return@launch
            }

            questionSender.sendQuestion(question).collect {
                when (it) {
                    is QuestionSender.Response.Success -> {
                        this@MainViewModel.response += it.text
                        _mainState.value = UIState.Answering(this@MainViewModel.response)
                        when (it.finishReason) {
                            FinishReason.Stop.value -> {
                                saveHistory()
                                _mainState.value = UIState.Idle(
                                    firstQuestion,
                                    question,
                                    this@MainViewModel.response,
                                    listened,
                                    isConnected
                                )
                            }

                            FinishReason.Length.value -> {
                                Log.w(TAG, it.finishReason)
                                _errorChannel.send(Error.SendQuestionError)
                                _mainState.value = UIState.Idle(firstQuestion, question, this@MainViewModel.response, listened, isConnected)
                            }
                        }
                    }

                    is QuestionSender.Response.Error -> {
                        Log.w(TAG, it.message)
                        _errorChannel.send(Error.SendQuestionError)
                        _mainState.value = UIState.Idle(firstQuestion, question, this@MainViewModel.response, listened, isConnected)
                    }

                    is QuestionSender.Response.UserCancel -> {
                        Log.i(TAG, "The user cancelled the job")
                    }
                }
            }
        }
    }

    private fun saveHistory() {
        history.add(ChatMessages(question, response))
        val jsonHistory = Gson().toJson(history)
        appSharedPreferences.saveHistoryJson(jsonHistory)
    }

    fun connectService(speechService: SpeechService) {
        speechServiceManager.connectService(speechService)
    }

    fun disconnectService() {
        speechServiceManager.disconnectService()
    }

    fun startListeningSpeechAndSpeechIfNeeded() {
        viewModelScope.launch {
            if (isConnected && _mainState.value is UIState.Listening) {
                speechServiceManager.recognizedText.observeForever(recognizedTextObserver)
                startRecording()
            }
        }
    }

    private fun resetRecording() {
        recordingByteArray = byteArrayOf()
    }

    fun setIDVoiceMessageGetter(idVoiceMessageGetter: IDVoiceMessageGetter) {
        this.idVoiceMessageGetter = idVoiceMessageGetter
    }

    companion object {
        val TAG = MainViewModel::class.qualifiedName
    }
}
