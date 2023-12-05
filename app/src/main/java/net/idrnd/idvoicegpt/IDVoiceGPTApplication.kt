package net.idrnd.idvoicegpt

import android.app.Application
import androidx.preference.PreferenceManager
import net.idrnd.idvoicegpt.idrnd.IdrndLicense
import net.idrnd.idvoicegpt.idrnd.LicenseStatus

class IDVoiceGPTApplication : Application() {

    private lateinit var voiceSdkLicense: IdrndLicense

    override fun onCreate() {
        super.onCreate()

        // Set OpenAI's API Key.
        val openAIPreferenceKey = getString(R.string.preference_open_ai_key)
        PreferenceManager.getDefaultSharedPreferences(this).apply {
            val openAiKey = getString(openAIPreferenceKey, null)
            if (openAiKey == null) {
                edit().putString(openAIPreferenceKey, BuildConfig.OPEN_AI_KEY).commit()
            }
        }

        // Set IDVoice license (the contents of VoiceSDK <release bundle>/license/license-*.txt file).
        voiceSdkLicense = IdrndLicense(BuildConfig.ID_VOICE_LICENSE)

        if (voiceSdkLicense.licenseStatus != LicenseStatus.Valid) {
            // We prevent user to use the application if the license is not valid,
            // so we don't need to initialize anything here.
            return
        }
    }
}
