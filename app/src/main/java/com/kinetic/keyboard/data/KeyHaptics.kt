package com.kinetic.keyboard.data

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import android.view.View

/**
 * P5.4: keypress vibration at a user-chosen [HapticStrength]. Shared by the IME (every key) and
 * the settings screen (a preview pulse when the user picks a level).
 */
class KeyHaptics(context: Context) {

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    /**
     * One-shot pulse sized by [strength]. Amplitude applies on motors with amplitude control;
     * the duration step keeps levels distinguishable on the rest. Devices without a vibrator
     * fall back to the view's KEYBOARD_TAP feedback (which the system may still honour).
     */
    fun vibrate(strength: HapticStrength, fallbackView: View? = null) {
        val v = vibrator
        if (v == null || !v.hasVibrator()) {
            fallbackView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        val amplitude = if (v.hasAmplitudeControl()) strength.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        v.vibrate(VibrationEffect.createOneShot(strength.durationMs, amplitude))
    }
}
