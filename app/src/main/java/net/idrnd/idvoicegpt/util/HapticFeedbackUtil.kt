package net.idrnd.idvoicegpt.util

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.content.SharedPreferences
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.preference.PreferenceManager
import net.idrnd.idvoicegpt.R

class HapticFeedbackUtil(context: Context) {

    private val settingsSharedPreferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val hapticFeedbackPreferenceKey = context.getString(R.string.preference_haptic_feedback_key)

    private val isVibrationEnabled: Boolean
        get() = settingsSharedPreferences.getBoolean(hapticFeedbackPreferenceKey, true)

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(VIBRATOR_SERVICE) as Vibrator
    }

    fun vibrate() {
        if (!isVibrationEnabled) {
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
        } else {
            vibrator.vibrate(10)
        }
    }
}
