package net.idrnd.idvoicegpt.ui.main

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.idrnd.IDVoiceManager
import net.idrnd.idvoicegpt.questionsender.DefaultQuestionSender
import net.idrnd.idvoicegpt.speaker.DefaultSpeaker
import net.idrnd.idvoicegpt.speechrecognition.DefaultSpeechServiceManager
import net.idrnd.idvoicegpt.voice.DefaultVoiceRecorderManager
import net.idrnd.idvoicegpt.voice.VoiceRecorder
import java.util.Locale

class MainViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    private val textToSpeechEngine: TextToSpeech by lazy {
        val languagePreferenceKey = context.getString(R.string.preference_language_key)
        TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeechEngine.language =
                    PreferenceManager.getDefaultSharedPreferences(context).getString(
                        languagePreferenceKey,
                        Locale.US.toLanguageTag()
                    )?.let { Locale.forLanguageTag(it) }
            }
        }
    }

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(MainViewModel::class.java)) {
            throw IllegalArgumentException("Unknown class name")
        }

        return MainViewModel(
            context,
            IDVoiceManager(context),
            DefaultSpeechServiceManager(),
            DefaultVoiceRecorderManager(VoiceRecorder()),
            DefaultQuestionSender(context),
            DefaultSpeaker(textToSpeechEngine)
        ) as T
    }
}
