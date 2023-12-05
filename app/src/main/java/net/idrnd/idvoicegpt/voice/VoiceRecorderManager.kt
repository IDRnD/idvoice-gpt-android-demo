package net.idrnd.idvoicegpt.voice

import kotlinx.coroutines.flow.Flow

/**
 *  Abstraction for voice recording.
 */
interface VoiceRecorderManager {

    /**
     * Start recording.
     *
     * @return A flow to stream recorded bytes wrapped in response objects.
     */
    fun startRecording(): Flow<Response>

    /**
     * @see VoiceRecorder.stop
     */
    fun stop()

    /**
     * @see VoiceRecorder.dismiss
     */
    fun dismiss()

    /**
     * Returns sample rate.
     */
    fun getSampleRate(): Int

    sealed class Response {
        data class Voice(val bytes: ByteArray, val chunkSize: Int) : Response()
        object VoiceStart : Response()
        object VoiceEnd : Response()
        data class Error(val message: String) : Response()
    }
}
