package net.idrnd.idvoicegpt.util

import android.content.SharedPreferences
import android.content.res.Resources
import net.idrnd.idvoicegpt.BuildConfig
import net.idrnd.idvoicegpt.R

const val HISTORY = "history_key"

fun SharedPreferences.getChatHistoryJson() = getString(HISTORY, null)
fun SharedPreferences.removeHistoryJson() = edit().remove(HISTORY).apply()
fun SharedPreferences.saveHistoryJson(history: String) = edit().putString(HISTORY, history).apply()

fun SharedPreferences.getPreferenceOpenAIKey(resources: Resources) = getString(
    resources.getString(R.string.preference_open_ai_key),
    BuildConfig.OPEN_AI_KEY
)
