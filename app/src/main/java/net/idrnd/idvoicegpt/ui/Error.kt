package net.idrnd.idvoicegpt.ui

sealed class Error {
    object SendQuestionError : Error()
    object RecordingError : Error()
    object RecognitionError : Error()
}
