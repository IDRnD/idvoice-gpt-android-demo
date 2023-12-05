package net.idrnd.idvoicegpt.idrnd

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import net.idrnd.idvoicegpt.R
import net.idrnd.voicesdk.android.media.AssetsExtractor
import net.idrnd.voicesdk.core.common.VoiceTemplate
import net.idrnd.voicesdk.liveness.LivenessEngine
import net.idrnd.voicesdk.verify.VoiceTemplateFactory
import net.idrnd.voicesdk.verify.VoiceTemplateMatcher
import java.io.File

/**
 * Creates and verifies voice template liveness and matches.
 */
class IDVoiceManager(
    context: Context
) {

    private val isCheckLivenessPreferenceKey = context.getString(R.string.preference_is_check_liveness_key)
    private val settingsSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    private var factory: VoiceTemplateFactory
    private var matcher: VoiceTemplateMatcher

    private var livenessEngine: LivenessEngine

    private var enrolledTemplate: VoiceTemplate? = null

    init {
        // Extract init data for liveness and verify engines.
        val initDataFolder = AssetsExtractor(context).extractAssets()

        // Make init data for biometrics modes.
        val voiceInitData =
            File(initDataFolder, AssetsExtractor.VERIFY_INIT_DATA_MIC_V1_SUBPATH).absolutePath
        factory = VoiceTemplateFactory(voiceInitData)
        matcher = VoiceTemplateMatcher(voiceInitData)

        val livenessInitData =
            File(initDataFolder, AssetsExtractor.LIVENESS_INIT_DATA_SUBPATH).absolutePath
        livenessEngine = LivenessEngine(livenessInitData)
    }

    /**
     * Creates a template with provided byte array,
     * then performs liveness verification if enabled,
     * finally verifies whether it matches with enrolled template.
     * In case there is no previous enrolled template then current template is enrolled.
     *
     * @param byteArray The recording bytes.
     * @param sampleRate The sample rate.
     */
    fun createAndVerifyTemplate(byteArray: ByteArray, sampleRate: Int): IDVoiceResponse {
        val voiceTemplate = factory.createVoiceTemplate(byteArray, sampleRate)
        val enrolledTemplateTemp = enrolledTemplate

        // If check liveness preference is on then we check liveness.
        if (settingsSharedPreferences.getBoolean(isCheckLivenessPreferenceKey, true)) {
            if (!isHuman(byteArray, sampleRate)) {
                return IDVoiceResponse.Failure.CheckLivenessFailed
            }
        }

        // If user already enrolled then we check match with new voice template.
        if (enrolledTemplateTemp != null) {
            return when (isMatch(enrolledTemplateTemp, voiceTemplate)) {
                true -> IDVoiceResponse.Success
                else -> IDVoiceResponse.Failure.MatchFailed
            }
        }

        // If user has not yet enrolled then we enroll received voice template.
        enrolledTemplate = voiceTemplate
        return IDVoiceResponse.Success
    }

    /**
     * Tells whether the recording bytes represent a human live recording or not.
     *
     * @param byteArray The recording byte array.
     * @return true if recording is live else false.
     */
    private fun isHuman(byteArray: ByteArray, sampleRate: Int) =
        livenessEngine.checkLiveness(byteArray, sampleRate).value.probability > 0.5

    /**
     * Takes two templates and verifies if they match.
     *
     * @param template1 The first template.
     * @param template2 The second template.
     */
    private fun isMatch(template1: VoiceTemplate, template2: VoiceTemplate) =
        matcher.matchVoiceTemplates(template1, template2).probability > 0.5

    /**
     * Resets enrolled template.
     */
    fun reset() {
        enrolledTemplate = null
    }
}
