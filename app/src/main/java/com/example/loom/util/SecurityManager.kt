package com.example.loom.util // --- FIX: Corrected package path ---

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit // --- FIX: Imported KTX edit extension ---
import androidx.fragment.app.FragmentActivity

object SecurityManager {
    private const val PREFS_NAME = "loom_security_prefs"
    private const val KEY_PIN = "custom_app_pin"

    // THE LOCK: Prevents multiple prompts from ever stacking on top of each other
    private var isAuthenticating = false

    private tailrec fun Context.getActivity(): FragmentActivity? = when (this) {
        is FragmentActivity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }

    fun getPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN, null)
    }

    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        // --- FIX: Using the fast-path KTX edit function ---
        prefs.edit { putString(KEY_PIN, pin) }
    }

    fun hasBiometricHardware(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(
        context: Context,
        title: String = "Unlock",
        subtitle: String = "Verify your identity to proceed",
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) {
        if (isAuthenticating) return

        val activity = context.getActivity()
        if (activity == null) {
            onFallback()
            return
        }

        isAuthenticating = true
        val executor = ContextCompat.getMainExecutor(activity)

        val prompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthenticating = false
                    Handler(Looper.getMainLooper()).postDelayed({ onFallback() }, 100)
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthenticating = false
                    Handler(Looper.getMainLooper()).postDelayed({ onSuccess() }, 250)
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use App PIN")
            .setConfirmationRequired(false)
            .build()

        prompt.authenticate(promptInfo)
    }
}