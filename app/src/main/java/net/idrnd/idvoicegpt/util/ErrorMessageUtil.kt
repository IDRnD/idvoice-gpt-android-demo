package net.idrnd.idvoicegpt.util

import android.content.Context
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.ui.Error

object ErrorMessageUtil {
    fun showErrorMessage(error: Error, context: Context) {
        val message = when (error) {
            is Error.RecordingError -> R.string.error_while_recording_voice
            is Error.RecognitionError -> R.string.error_while_recognizing_voice
            is Error.SendQuestionError -> R.string.error_while_sending_question
        }.let {
            context.getString(it)
        }
        context.toast(message)
    }
}
