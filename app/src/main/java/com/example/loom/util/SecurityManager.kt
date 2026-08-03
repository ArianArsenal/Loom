package com.example.loom.utils // Adjust package name as needed

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object SecurityManager {
    private const val PREFS_NAME = "loom_security_prefs"
    private const val KEY_PIN = "custom_app_pin"

    // Retrieve the saved PIN (Returns null if the user hasn't set one yet)
    fun getPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN, null)
    }

    // Save the new PIN
    fun savePin(context: Context, pin: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_PIN, pin).apply()
    }

    // Global Biometric Prompt that any screen can call
    fun authenticate(
        context: Context,
        title: String = "Unlock",
        subtitle: String = "Verify your identity to proceed",
        onSuccess: () -> Unit,
        onFallback: () -> Unit
    ) {
        val activity = context as? FragmentActivity
        if (activity == null) {
            onFallback()
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val biometricPrompt = BiometricPrompt(activity, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onFallback() // User clicked "Use App PIN" or fingerprint failed/canceled
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText("Use App PIN")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}