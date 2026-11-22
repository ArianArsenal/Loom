package com.example.loom.util // --- FIX: Corrected package path ---

import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.edit // --- FIX: Imported KTX edit extension ---
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

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

    private const val KEY_FAILED_ATTEMPTS = "pin_failed_attempts"
    private const val KEY_LOCKED_UNTIL = "pin_locked_until"
    private const val HASH_PREFIX = "pbkdf2$"
    private const val HASH_ITERATIONS = 20_000
    private const val FREE_ATTEMPTS = 5

    /**
     * Returns the stored PIN record (a salted hash), or null if none is set. Only use it to check
     * whether a PIN exists; compare PINs with [verifyPin].
     */
    fun getPin(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_PIN, null)?.takeIf { it.isNotBlank() }
    }

    /** Stores a salted PBKDF2 hash of [pin]; a blank pin removes it. */
    fun savePin(context: Context, pin: String) {
        if (pin.isBlank()) {
            clearPin(context)
            return
        }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_PIN, hashPin(pin)) }
    }

    /**
     * Checks [input] against the stored PIN. Older versions stored the PIN in plain text: such a
     * value is accepted once and immediately upgraded to a hash. After [FREE_ATTEMPTS] wrong tries,
     * further attempts are refused for an exponentially growing period.
     */
    fun verifyPin(context: Context, input: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_PIN, null)?.takeIf { it.isNotBlank() } ?: return false
        if (System.currentTimeMillis() < prefs.getLong(KEY_LOCKED_UNTIL, 0L)) return false

        val matches = if (stored.startsWith(HASH_PREFIX)) {
            val parts = stored.removePrefix(HASH_PREFIX).split("$")
            parts.size == 2 && MessageDigest.isEqual(
                pbkdf2(input, Base64.decode(parts[0], Base64.NO_WRAP)),
                Base64.decode(parts[1], Base64.NO_WRAP)
            )
        } else {
            MessageDigest.isEqual(stored.toByteArray(), input.toByteArray())
        }

        if (matches) {
            prefs.edit {
                remove(KEY_FAILED_ATTEMPTS)
                remove(KEY_LOCKED_UNTIL)
                if (!stored.startsWith(HASH_PREFIX)) putString(KEY_PIN, hashPin(input))
            }
        } else {
            val failures = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            prefs.edit {
                putInt(KEY_FAILED_ATTEMPTS, failures)
                if (failures >= FREE_ATTEMPTS) {
                    val lockSeconds = (30L shl (failures - FREE_ATTEMPTS).coerceAtMost(5)) // 30s .. 16min
                    putLong(KEY_LOCKED_UNTIL, System.currentTimeMillis() + lockSeconds * 1000)
                }
            }
        }
        return matches
    }

    private fun hashPin(pin: String): String {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return HASH_PREFIX + Base64.encodeToString(salt, Base64.NO_WRAP) + "$" +
            Base64.encodeToString(pbkdf2(pin, salt), Base64.NO_WRAP)
    }

    private fun pbkdf2(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, HASH_ITERATIONS, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    fun clearPin(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            remove(KEY_PIN)
            remove(KEY_FAILED_ATTEMPTS)
            remove(KEY_LOCKED_UNTIL)
        }
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

        try {
            prompt.authenticate(promptInfo)
        } catch (e: Exception) {
            // Never leave the lock set (it would block every later prompt) or the caller waiting
            isAuthenticating = false
            onFallback()
        }
    }
}