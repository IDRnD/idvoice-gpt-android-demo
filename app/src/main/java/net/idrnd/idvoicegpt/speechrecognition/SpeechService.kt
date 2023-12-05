package net.idrnd.idvoicegpt.speechrecognition

import android.app.Service
import android.content.Intent
import android.content.SharedPreferences
import android.os.Binder
import android.os.IBinder
import android.text.TextUtils
import android.util.Log
import androidx.preference.PreferenceManager
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechGrpc
import com.google.cloud.speech.v1.SpeechGrpc.SpeechStub
import com.google.cloud.speech.v1.StreamingRecognitionConfig
import com.google.cloud.speech.v1.StreamingRecognizeRequest
import com.google.cloud.speech.v1.StreamingRecognizeResponse
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelProvider
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.idrnd.idvoicegpt.R
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Speech recognition service that uses Google Cloud Speech to Text.
 */
class SpeechService : Service() {

    private var binder: SpeechBinder? = null
    private val speechRecognitionListeners = ArrayList<SpeechRecognitionListener>()

    private var googleCloudSpeechClient: SpeechStub? = null
    private var sharedPreferences: SharedPreferences? = null
    private val responseObserver: StreamObserver<StreamingRecognizeResponse> =
        object : StreamObserver<StreamingRecognizeResponse> {
            override fun onNext(response: StreamingRecognizeResponse) {
                var text: String? = null
                var isFinal = false
                if (response.resultsCount > 0) {
                    val result = response.getResults(0)
                    isFinal = result.isFinal
                    if (result.alternativesCount > 0) {
                        val alternative = result.getAlternatives(0)
                        text = alternative.transcript
                        Log.i(TAG, "Text: $text")
                    }
                }
                if (text != null) {
                    for (listener in speechRecognitionListeners) {
                        listener.onSpeechRecognized(text, isFinal)
                    }
                }
            }

            override fun onError(t: Throwable) {
                Log.e(TAG, "Error calling the API.", t)
                broadcastError(API_FAILED_CALL)
            }

            override fun onCompleted() {
                Log.i(TAG, "API completed.")
            }
        }

    private var requestObserver: StreamObserver<StreamingRecognizeRequest>? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private val accessTokenManager: AccessTokenManager by lazy {
        AccessTokenManager(
            getSharedPreferences(PREFERENCES_KEY, MODE_PRIVATE),
            resources
        )
    }

    private val defaultLanguageCode: String
        get() {
            val languagePreferenceKey = getString(R.string.preference_language_key)
            val languageTag = sharedPreferences?.getString(languagePreferenceKey, null)
            val locale = when(languageTag){
                null -> Locale.US.toLanguageTag()
                else -> languageTag
            }.let {
                Locale.forLanguageTag(it)
            }
            val language = StringBuilder(locale.language)
            val country = locale.country
            if (!TextUtils.isEmpty(country)) {
                language.append("-")
                language.append(country)
            }
            return language.toString()
        }

    override fun onCreate() {
        super.onCreate()
        binder = SpeechBinder()
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setGoogleCloudSpeechClient()
    }

    override fun onDestroy() {
        super.onDestroy()
        binder = null
        sharedPreferences = null
        serviceScope.cancel()
        // Release the gRPC channel.
        if (googleCloudSpeechClient == null) {
            return
        }
        val channel = googleCloudSpeechClient?.channel as ManagedChannel
        if (channel.isShutdown) {
            return
        }
        try {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error shutting down the gRPC channel.", e)
        }
        googleCloudSpeechClient = null
    }

    private fun setGoogleCloudSpeechClient() {
        serviceScope.launch {
            val accessToken = accessTokenManager.accessToken
            if (accessToken == null) {
                Log.w(TAG, getString(R.string.access_token_is_null))
                broadcastError(ACCESS_TOKEN_NULL)
                return@launch
            }

            val channel = OkHttpChannelProvider()
                .builderForAddress(HOSTNAME, PORT)
                .intercept(
                    GoogleCredentialsInterceptor(GoogleCredentials(accessToken).createScoped(SCOPE))
                )
                .build()
            googleCloudSpeechClient = SpeechGrpc.newStub(channel)
            scheduleGoogleCloudSpeechUpdate(accessToken)
        }
    }

    private fun broadcastError(type: Int) {
        val intent = Intent(ACTION_SPEECH_SERVICE_ERROR)
        intent.putExtra(SPEECH_SERVICE_ERROR_TYPE, type)
        sendBroadcast(intent)
    }

    private fun scheduleGoogleCloudSpeechUpdate(accessToken: AccessToken) {
        serviceScope.launch {
            // Schedule access token refresh before it expires.
            delay(
                Math.max(
                    accessToken.expirationTime.time -
                        System.currentTimeMillis() -
                        AccessTokenManager.ACCESS_TOKEN_FETCH_MARGIN,
                    AccessTokenManager.ACCESS_TOKEN_EXPIRATION_TOLERANCE.toLong()
                )
            )
            setGoogleCloudSpeechClient()
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    fun addListener(speechRecognitionListener: SpeechRecognitionListener) {
        speechRecognitionListeners.add(speechRecognitionListener)
    }

    fun removeListener(speechRecognitionListener: SpeechRecognitionListener) {
        speechRecognitionListeners.remove(speechRecognitionListener)
    }

    /**
     * Starts recognizing speech audio.
     *
     * @param sampleRate The sample rate of the audio.
     */
    fun startRecognizing(sampleRate: Int) {
        if (googleCloudSpeechClient == null) {
            val message = getString(R.string.api_not_ready)
            Log.w(TAG, message)
            broadcastError(API_NOT_READY)
            return
        }
        Log.w(TAG, "startRecognizing")
        // Configure the API.
        requestObserver = googleCloudSpeechClient?.streamingRecognize(responseObserver)
        requestObserver?.onNext(
            StreamingRecognizeRequest.newBuilder()
                .setStreamingConfig(
                    StreamingRecognitionConfig.newBuilder()
                        .setConfig(
                            RecognitionConfig.newBuilder()
                                .setLanguageCode(defaultLanguageCode)
                                .setEncoding(RecognitionConfig.AudioEncoding.LINEAR16)
                                .setSampleRateHertz(sampleRate)
                                .build()
                        )
                        .setInterimResults(true)
                        .setSingleUtterance(true)
                        .build()
                )
                .build()
        )
    }

    /**
     * Recognizes the speech audio. This method should be called every time a chunk of byte buffer
     * is ready.
     *
     * @param data The audio data.
     * @param size The number of elements that are actually relevant in the `data`.
     */
    fun recognize(data: ByteArray?, size: Int) {
        if (requestObserver == null) {
            return
        }
        // Call the streaming recognition API.
        requestObserver?.onNext(
            StreamingRecognizeRequest.newBuilder()
                .setAudioContent(ByteString.copyFrom(data, 0, size))
                .build()
        )
    }

    /**
     * Finishes recognizing speech audio.
     */
    fun finishRecognizing() {
        if (requestObserver == null) {
            return
        }
        requestObserver?.onCompleted()
        requestObserver = null
    }

    private inner class SpeechBinder : Binder() {
        fun getService(): SpeechService = this@SpeechService
    }

    companion object {
        private const val TAG = "SpeechService"
        private const val PREFERENCES_KEY = "SpeechService"

        val SCOPE = listOf("https://www.googleapis.com/auth/cloud-platform")
        private const val HOSTNAME = "speech.googleapis.com"
        private const val PORT = 443

        const val ACTION_SPEECH_SERVICE_ERROR = "ACTION_SPEECH_SERVICE_ERROR"
        const val SPEECH_SERVICE_ERROR_TYPE = "SPEECH_SERVICE_ERROR_TYPE"

        const val ACCESS_TOKEN_NULL = 0
        const val API_NOT_READY = 1
        const val API_FAILED_CALL = 2

        const val ACTION_GOOGLE_CLOUD_SPEECH_CLIENT_NOT_READY =
            "ACTION_GOOGLE_CLOUD_SPEECH_CLIENT_NOT_READY"

        fun from(binder: IBinder): SpeechService {
            return (binder as SpeechBinder).getService()
        }
    }
}
