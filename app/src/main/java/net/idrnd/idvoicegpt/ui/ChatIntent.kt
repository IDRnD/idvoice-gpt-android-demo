package net.idrnd.idvoicegpt.ui

/**
 * MVI intents the user/system can trigger
 */
sealed class ChatIntent {
    object CleanUpAppIntent : ChatIntent()
    object ListenIntent : ChatIntent()
    object CancelListeningIntent : ChatIntent()
    object SendQuestionIntent : ChatIntent()
    data class StopAnsweringIntent(val partialText: String) : ChatIntent()
    object Speak : ChatIntent()
    data class ConnectionUpdateIntent(val isConnected: Boolean) : ChatIntent()
}
