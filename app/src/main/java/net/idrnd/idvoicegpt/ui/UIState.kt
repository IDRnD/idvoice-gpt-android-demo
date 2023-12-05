package net.idrnd.idvoicegpt.ui

/**
 * The UI states of the app
 */
sealed class UIState {
    data class Idle(
        val showLogo: Boolean,
        val question: String,
        val response: String,
        val listened: String,
        val isConnected: Boolean
    ) : UIState()

    data class Listening(
        val listened: String,
        val question: String,
        val response: String,
        val showLogo: Boolean
    ) : UIState()

    data class Thinking(val userQuestion: String) : UIState()
    data class Answering(val answer: String) : UIState()
}
