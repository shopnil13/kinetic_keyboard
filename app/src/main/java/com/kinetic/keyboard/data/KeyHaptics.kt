package com.kinetic.keyboard.data

import android.content.Context
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RequiresApi

/**
 * P5.4: keypress vibration at a user-chosen [HapticStrength]. Shared by the IME (every key) and
 * the settings screen (a preview pulse when the user picks a level).
 *
 * Why it is more than a one-shot (keypresses were weak or missing on newer phones):
 *  - Linear (LRA) motors, standard on recent devices, barely register an 8–14 ms one-shot. Where
 *    the motor supports it we play the click primitive / predefined click effects it is tuned
 *    for; the one-shot stays as the fallback for older (ERM) motors, long enough to spin up.
 *  - Android 13+ treats a short vibration without attributes as touch feedback and drops it
 *    whenever the system "Touch feedback" switch is off — silencing the keyboard even though its
 *    own "Vibrate on keypress" is on. See [usage].
 */
class KeyHaptics(context: Context) {

    private val resolver = context.contentResolver

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    // Hardware capabilities, probed once: each query is a binder call and never changes.
    private val hasVibrator = vibrator?.hasVibrator() == true
    private val hasAmplitudeControl = hasVibrator && vibrator?.hasAmplitudeControl() == true
    private val clickPrimitive = hasVibrator && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        vibrator?.areAllPrimitivesSupported(VibrationEffect.Composition.PRIMITIVE_CLICK) == true
    private val predefinedClicks = hasVibrator && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        vibrator?.areAllEffectsSupported(
            VibrationEffect.EFFECT_TICK, VibrationEffect.EFFECT_CLICK, VibrationEffect.EFFECT_HEAVY_CLICK,
        ) == Vibrator.VIBRATION_EFFECT_SUPPORT_YES

    /**
     * One click sized by [strength]. Devices without a vibrator fall back to the view's
     * KEYBOARD_TAP feedback (which the system may still honour).
     */
    fun vibrate(strength: HapticStrength, fallbackView: View? = null) {
        val v = vibrator
        if (v == null || !hasVibrator) {
            fallbackView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            return
        }
        val effect = effectFor(strength)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            v.vibrate(effect, VibrationAttributes.createForUsage(usage()))
        } else {
            v.vibrate(effect)
        }
    }

    private fun effectFor(strength: HapticStrength): VibrationEffect {
        // Best: the motor's own click, scaled — crisp on LRAs and the levels stay distinct.
        if (clickPrimitive && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return VibrationEffect.startComposition()
                .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, strength.clickScale)
                .compose()
        }
        // Next: vendor-tuned click effects, only when the HAL confirms them (an "unknown" answer
        // can play a long generic fallback buzz instead).
        if (predefinedClicks && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return VibrationEffect.createPredefined(
                when (strength) {
                    HapticStrength.LOW -> VibrationEffect.EFFECT_TICK
                    HapticStrength.MEDIUM -> VibrationEffect.EFFECT_CLICK
                    HapticStrength.HIGH -> VibrationEffect.EFFECT_HEAVY_CLICK
                },
            )
        }
        // Fallback: timed pulse. Amplitude where the motor supports it; the duration step keeps
        // the levels apart on motors without amplitude control.
        val amplitude = if (hasAmplitudeControl) strength.amplitude else VibrationEffect.DEFAULT_AMPLITUDE
        return VibrationEffect.createOneShot(strength.durationMs, amplitude)
    }

    /**
     * Android 13+: touch usage follows the system touch-intensity setting, so use it while the
     * system "Touch feedback" switch is on. With that switch off the system drops touch-usage
     * vibrations entirely; the keyboard's own "Vibrate on keypress" switch is the user's explicit
     * choice for key presses, so play through the media channel then (its own system setting
     * still applies).
     */
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun usage(): Int {
        val touchFeedbackOn = runCatching {
            Settings.System.getInt(resolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        }.getOrDefault(true)
        return if (touchFeedbackOn) VibrationAttributes.USAGE_TOUCH else VibrationAttributes.USAGE_MEDIA
    }
}
