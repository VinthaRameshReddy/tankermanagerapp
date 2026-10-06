package com.tankermanager.app.util

import android.util.Base64
import com.tankermanager.app.BuildConfig
import org.json.JSONObject
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Client-side security helpers:
 * - SHA-256 digest of MPIN (raw MPIN never sent / never stored)
 * - AES-256-GCM payload encoding for auth APIs
 */
object SecureCrypto {
    private const val IV_LEN = 12
    private const val TAG_BITS = 128

    fun sha256Hex(input: String): String {
        val dig = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return dig.joinToString("") { "%02x".format(it) }
    }

    /** Digests a 4–6 digit MPIN with SHA-256. Does not persist anything. */
    fun mpinDigest(mpin: String): String {
        require(mpin.matches(Regex("^\\d{4,6}$"))) { "MPIN must be 4–6 digits" }
        return sha256Hex(mpin)
    }

    fun encryptJson(json: String): String {
        val key = deriveKey(BuildConfig.PAYLOAD_KEY)
        val iv = ByteArray(IV_LEN).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv))
        val cipherText = cipher.doFinal(json.toByteArray(Charsets.UTF_8))
        val all = ByteBuffer.allocate(iv.size + cipherText.size).put(iv).put(cipherText).array()
        return Base64.encodeToString(all, Base64.NO_WRAP)
    }

    fun encryptObject(vararg pairs: Pair<String, Any?>): String {
        val obj = JSONObject()
        pairs.forEach { (k, v) ->
            if (v != null) obj.put(k, v)
        }
        return encryptJson(obj.toString())
    }

    private fun deriveKey(secret: String): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(secret.toByteArray(Charsets.UTF_8))
    }
}
