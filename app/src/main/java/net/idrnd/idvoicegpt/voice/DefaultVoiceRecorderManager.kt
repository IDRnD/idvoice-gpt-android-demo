package net.idrnd.idvoicegpt.voice

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Records voice using the voice recorder.
 */
class DefaultVoiceRecorderManager(private val voiceRecorder: VoiceRecorder) : VoiceRecorderManager {

    override fun startRecording(): Flow<VoiceRecorderManager.Response> = callbackFlow {
        val voiceRecorderCallback = object : VoiceRecorder.VoiceRecorderCallback {
            override fun onVoiceStart() {
                trySend(VoiceRecorderManager.Response.VoiceStart)
            }

            override fun onVoice(data: ByteArray, size: Int) {
                trySend(VoiceRecorderManager.Response.Voice(data, size))
            }

            override fun onVoiceEnd() {
                trySend(VoiceRecorderManager.Response.VoiceEnd)
            }
        }
        voiceRecorder.voiceRecorderCallback = voiceRecorderCallback

        voiceRecorder.start()
        awaitClose { voiceRecorderCallback.onVoiceEnd() }
    }

    override fun stop() {
        voiceRecorder.stop()
    }

    override fun dismiss() {
        voiceRecorder.dismiss()
    }

    override fun getSampleRate(): Int = voiceRecorder.sampleRate
}
