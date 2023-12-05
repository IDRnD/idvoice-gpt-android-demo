package net.idrnd.idvoicegpt.questionsender

import kotlinx.coroutines.flow.Flow

/**
 * An abstraction that can perform a question.
 */
interface QuestionSender {

    /**
     * Sends a question and return an stream of text chunks.
     *
     * @param question The question to send.
     * @return The stream of text chunks
     */
    suspend fun sendQuestion(question: String): Flow<Response>

    sealed class Response {
        data class Success(val text: String, val finishReason: String?) : Response()
        data class Error(val message: String) : Response()
        object UserCancel : Response()
    }
}
