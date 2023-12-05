package net.idrnd.idvoicegpt.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder.AudioSource

/**
 * Records voice turning it into bytes
 */
class VoiceRecorder {
    interface VoiceRecorderCallback {
        /**
         * Called when the recorder starts hearing voice.
         */
        fun onVoiceStart()

        /**
         * Called when the recorder is hearing voice.
         *
         * @param data The audio data in [AudioFormat.ENCODING_PCM_16BIT].
         * @param size The size of the actual data in `data`.
         */
        fun onVoice(data: ByteArray, size: Int)

        /**
         * Called when the recorder stops hearing voice.
         */
        fun onVoiceEnd()
    }

    var voiceRecorderCallback: VoiceRecorderCallback? = null
    private var audioRecord: AudioRecord? = null
    private var thread: Thread? = null
    private var buffer: ByteArray = byteArrayOf()
    private val lock = Any()

    /**
     * The timestamp of the last time that voice is heard.
     */
    private var lastVoiceHeardMillis = Long.MAX_VALUE

    /**
     * The timestamp when the current voice is started.
     */
    private var voiceStartedMillis: Long = 0

    /**
     * Starts recording audio.
     *
     *
     * The caller is responsible for calling [.stop] later.
     */
    fun start() {
        // Stop recording if it is currently ongoing.
        stop()
        // Try to create a new recording session.
        audioRecord = createAudioRecord()
        if (audioRecord == null) {
            throw RuntimeException("Cannot instantiate VoiceRecorder")
        }
        // Start recording.
        audioRecord?.startRecording()
        // Start processing the captured audio.
        thread = Thread(ProcessVoice())
        thread?.start()
    }

    /**
     * Stops recording audio.
     */
    fun stop() {
        synchronized(lock) {
            dismiss()
            thread?.interrupt()
            thread = null
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            buffer = byteArrayOf()
        }
    }

    /**
     * Dismisses the currently ongoing utterance.
     */
    fun dismiss() {
        if (lastVoiceHeardMillis != Long.MAX_VALUE) {
            lastVoiceHeardMillis = Long.MAX_VALUE
            voiceRecorderCallback?.onVoiceEnd()
        }
    }

    val sampleRate: Int
        /**
         * Retrieves the sample rate currently used to record audio.
         *
         * @return The sample rate of recorded audio.
         */
        get() = SAMPLE_RATE

    /**
     * Creates a new [AudioRecord].
     *
     * @return A newly created [AudioRecord], or null if it cannot be created (missing
     * permissions?).
     */
    private fun createAudioRecord(): AudioRecord? {
        val sizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING)

        @SuppressLint("MissingPermission") // We already handle permissions in UI
        val audioRecord = AudioRecord(
            AudioSource.DEFAULT,
            SAMPLE_RATE,
            CHANNEL,
            ENCODING,
            sizeInBytes
        )
        if (audioRecord.state == AudioRecord.STATE_INITIALIZED) {
            buffer = ByteArray(sizeInBytes)
            return audioRecord
        } else {
            audioRecord.release()
        }
        return null
    }

    /**
     * Continuously processes the captured audio and notifies [.mCallback] of corresponding
     * events.
     */
    private inner class ProcessVoice : Runnable {
        override fun run() {
            while (true) {
                synchronized(lock) {
                    if (!Thread.currentThread().isInterrupted) {
                        val size = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                        val now = System.currentTimeMillis()
                        if (isHearing(buffer, size)) {
                            if (lastVoiceHeardMillis == Long.MAX_VALUE) {
                                voiceStartedMillis = now
                                voiceRecorderCallback?.onVoiceStart()
                            }
                            voiceRecorderCallback?.onVoice(buffer, size)
                            lastVoiceHeardMillis = now
                            if (now - voiceStartedMillis > MAX_SPEECH_LENGTH_MILLIS) {
                                end()
                            }
                        } else if (lastVoiceHeardMillis != Long.MAX_VALUE) {
                            voiceRecorderCallback?.onVoice(buffer, size)
                            if (now - lastVoiceHeardMillis > SPEECH_TIMEOUT_MILLIS) {
                                end()
                            }
                        }
                    }
                }
            }
        }

        private fun end() {
            lastVoiceHeardMillis = Long.MAX_VALUE
            voiceRecorderCallback?.onVoiceEnd()
        }

        private fun isHearing(buffer: ByteArray, size: Int): Boolean {
            var i = 0
            while (i < size - 1) {
                // The buffer has LINEAR16 in little endian.
                var s = buffer[i + 1].toInt()
                if (s < 0) s *= -1
                s = s shl 8
                s += Math.abs(buffer[i].toInt())
                if (s > AMPLITUDE_THRESHOLD) {
                    return true
                }
                i += 2
            }
            return false
        }
    }

    companion object {
        private const val SAMPLE_RATE = 48000
        private const val CHANNEL = AudioFormat.CHANNEL_IN_MONO
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val AMPLITUDE_THRESHOLD = 1500
        private const val SPEECH_TIMEOUT_MILLIS = 1000
        private const val MAX_SPEECH_LENGTH_MILLIS = 30 * 1000
    }
}
