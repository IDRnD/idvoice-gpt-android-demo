package net.idrnd.idvoicegpt.ui.settings

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.core.os.LocaleListCompat
import androidx.preference.DropDownPreference
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.Preference.SummaryProvider
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.PreferenceManager
import net.idrnd.idvoicegpt.BuildConfig
import net.idrnd.idvoicegpt.R
import java.util.Locale

/**
 * The settings screen.
 */
class SettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences, rootKey)
        activity?.findViewById<Toolbar>(R.id.main_toolbar)?.title = getString(R.string.settings)

        setOpenAIKeyPreference()
        setLanguagePreference()
    }

    private fun setOpenAIKeyPreference() {
        val openAIKeyPreferenceKey = getString(R.string.preference_open_ai_key)
        val openAIKey = PreferenceManager.getDefaultSharedPreferences(requireContext())
            .getString(openAIKeyPreferenceKey, BuildConfig.OPEN_AI_KEY)

        val editPreference = findPreference<EditTextPreference>(openAIKeyPreferenceKey)
        editPreference?.apply {
            summaryProvider = SummaryProvider<Preference> {
                openAIKey?.let { "*".repeat(it.length) }
            }

            setOnBindEditTextListener { editTextView ->
                editTextView.inputType =
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                summaryProvider = SummaryProvider<Preference> {
                    "*".repeat(editTextView.text.toString().length)
                }
            }
        }
    }

    private fun setLanguagePreference() {
        val languagePreference = findPreference<DropDownPreference>(getString(R.string.preference_language_key))
        languagePreference?.setOnPreferenceChangeListener { _, newValue ->
            AppCompatDelegate.setApplicationLocales(
                LocaleListCompat.create(Locale.forLanguageTag(newValue as String))
            )
            true
        }
    }
}
