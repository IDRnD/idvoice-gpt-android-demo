package net.idrnd.idvoicegpt.questionsender

import android.content.Context
import androidx.preference.PreferenceManager
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.exception.GenericIOException
import com.aallam.openai.api.exception.OpenAITimeoutException
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import net.idrnd.idvoicegpt.R
import net.idrnd.idvoicegpt.util.getPreferenceOpenAIKey
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException
import kotlin.time.Duration.Companion.seconds

/**
 * Sends a question to ChatGPT using OpenAI client.
 */
class DefaultQuestionSender(
    context: Context
) : QuestionSender {

    private val settingsSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val resources = context.resources

    private val invalidOpenAIKeyError = resources.getString(R.string.invalid_open_ai_key)
    private val systemMessageContent = resources.getString(R.string.gpt_system_message_content)
    private val chatSystemMessage = ChatMessage(
        role = ChatRole.System,
        content = systemMessageContent
    )

    private val socketTimeoutErrorMessage
        get() = resources.getString(R.string.please_check_your_connectivity_and_try_again)
    private val connectionErrorMessage
        get() = resources.getString(R.string.connection_error)
    private val unknownErrorMessage
        get() = resources.getString(R.string.unknown_error)
    private val interruptedErrorMessage
        get() = resources.getString(R.string.operation_interrupted)

    override suspend fun sendQuestion(question: String): Flow<QuestionSender.Response> {
        val openAiKey = settingsSharedPreferences.getPreferenceOpenAIKey(resources)

        if (openAiKey.isNullOrBlank()) {
            throw IllegalStateException(invalidOpenAIKeyError)
        }

        val config = OpenAIConfig(
            token = openAiKey,
            timeout = Timeout(socket = SOCKET_TIMEOUT)
        )

        val openAI = OpenAI(config)
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(MODEL_ID),
            messages = listOf(
                chatSystemMessage,
                ChatMessage(
                    role = ChatRole.User,
                    content = question
                )
            )
        )
        return openAI.chatCompletions(chatCompletionRequest).map {
            val word = it.choices.firstOrNull()?.delta?.content ?: ""
            val finishReason = it.choices.firstOrNull()?.finishReason?.value
            QuestionSender.Response.Success(word, finishReason) as QuestionSender.Response
        }.catch {
            val errorResponse = when (it) {
                is SocketTimeoutException, is OpenAITimeoutException -> QuestionSender.Response.Error(
                    socketTimeoutErrorMessage
                )
                is UnknownHostException, is GenericIOException -> QuestionSender.Response.Error(
                    connectionErrorMessage
                )
                is InterruptedIOException -> QuestionSender.Response.Error(interruptedErrorMessage)
                is CancellationException -> QuestionSender.Response.UserCancel
                else -> QuestionSender.Response.Error("$unknownErrorMessage: ${it.message}")
            }
            emit(errorResponse)
        }
    }

    companion object {
        val TAG = DefaultQuestionSender::class.qualifiedName
        private val SOCKET_TIMEOUT = 20.seconds
        private const val MODEL_ID = "gpt-3.5-turbo"
    }
}
