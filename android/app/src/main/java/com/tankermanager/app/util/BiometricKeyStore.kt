package com.tankermanager.app.util

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Hardware-backed EC key gated by biometric. Private key never leaves Keystore.
 * Backend stores only the public key and verifies challenge signatures.
 */
object BiometricKeyStore {
    private const val ALIAS = "tankerflow_biometric_ec"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    fun canAuthenticate(context: Context): Boolean {
        val mgr = BiometricManager.from(context)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        return mgr.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun hasKey(): Boolean {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return ks.containsAlias(ALIAS)
    }

    fun deleteKey() {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (ks.containsAlias(ALIAS)) ks.deleteEntry(ALIAS)
    }

    /** Creates (or replaces) a biometric-bound EC P-256 keypair. Returns public key Base64 (X.509). */
    fun createKeyPair(): String {
        deleteKey()
        val generator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, ANDROID_KEYSTORE)
        val builder = KeyGenParameterSpec.Builder(
            ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.initialize(builder.build())
        val pair = generator.generateKeyPair()
        return Base64.encodeToString(pair.public.encoded, Base64.NO_WRAP)
    }

    fun publicKeyBase64(): String? {
        val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val entry = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry ?: return null
        return Base64.encodeToString(entry.certificate.publicKey.encoded, Base64.NO_WRAP)
    }

    /**
     * Prompts biometric and signs [payload] with the Keystore private key.
     * Returns Base64 DER ECDSA signature.
     */
    suspend fun signWithBiometric(
        activity: FragmentActivity,
        payload: String,
        title: String = "Confirm it's you",
        subtitle: String = "Use biometrics to sign in securely"
    ): String = suspendCoroutine { cont ->
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val entry = ks.getEntry(ALIAS, null) as? KeyStore.PrivateKeyEntry
                ?: throw IllegalStateException("Biometric key not registered")
            val signature = Signature.getInstance("SHA256withECDSA").apply {
                initSign(entry.privateKey)
            }
            val crypto = BiometricPrompt.CryptoObject(signature)
            val executor = ContextCompat.getMainExecutor(activity)
            val prompt = BiometricPrompt(
                activity,
                executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        try {
                            val sig = result.cryptoObject?.signature
                                ?: throw IllegalStateException("Missing crypto object")
                            sig.update(payload.toByteArray(Charsets.UTF_8))
                            val bytes = sig.sign()
                            cont.resume(Base64.encodeToString(bytes, Base64.NO_WRAP))
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        cont.resumeWithException(IllegalStateException(errString.toString()))
                    }

                    override fun onAuthenticationFailed() {
                        // keep prompt open; user can retry
                    }
                }
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText("Cancel")
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                .build()
            prompt.authenticate(info, crypto)
        } catch (e: Exception) {
            cont.resumeWithException(e)
        }
    }
}
