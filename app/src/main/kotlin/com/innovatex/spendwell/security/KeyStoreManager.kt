package com.innovatex.spendwell.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import com.innovatex.spendwell.database.SpendwellDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages hardware-backed encryption keys via the Android Keystore with resilient fallbacks.
 * Generates and securely protects the 256-bit SQLCipher database encryption passphrase.
 */
@Singleton
class KeyStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "KeyStoreManager"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SpendwellDatabaseMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_FILE = "spendwell_db_passphrase.enc"
        private const val IV_FILE = "spendwell_db_passphrase.iv"
        private const val FALLBACK_KEY_FILE = "spendwell_db_fallback.key"
        private const val KEY_SIZE = 256
    }

    private var keyStore: KeyStore? = null

    init {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
                load(null)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "AndroidKeyStore unavailable, using app-private secure fallback: ${t.message}")
            keyStore = null
        }
    }

    /**
     * Retrieves or generates the 256-bit SQLCipher passphrase.
     * The raw passphrase is encrypted with an Android Keystore hardware-backed AES key
     * and persisted locally in private app storage.
     * If decryption fails (e.g. after fresh install with restored files), the corrupt
     * data is cleaned up and a fresh valid passphrase is created safely.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val passphraseFile = File(context.filesDir, PASSPHRASE_FILE)
        val ivFile = File(context.filesDir, IV_FILE)

        if (passphraseFile.exists() && ivFile.exists()) {
            try {
                val encryptedBytes = passphraseFile.readBytes()
                val iv = ivFile.readBytes()
                val decrypted = decrypt(encryptedBytes, iv)
                if (decrypted.isNotEmpty()) {
                    return decrypted
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Passphrase decryption failed (stale or restored key), resetting: ${t.message}")
                passphraseFile.delete()
                ivFile.delete()
                try {
                    context.deleteDatabase(SpendwellDatabase.DATABASE_NAME)
                } catch (ignored: Throwable) {}
            }
        }

        // Try to generate via Keystore
        try {
            val rawPassphrase = ByteArray(32)
            SecureRandom().nextBytes(rawPassphrase)

            val (encryptedBytes, iv) = encrypt(rawPassphrase)
            passphraseFile.writeBytes(encryptedBytes)
            ivFile.writeBytes(iv)
            return rawPassphrase
        } catch (t: Throwable) {
            Log.e(TAG, "Keystore encryption failed, falling back to private storage key: ${t.message}")
            return getOrCreateFallbackPassphrase()
        }
    }

    private fun getOrCreateFallbackPassphrase(): ByteArray {
        val fallbackFile = File(context.filesDir, FALLBACK_KEY_FILE)
        if (fallbackFile.exists()) {
            try {
                val bytes = fallbackFile.readBytes()
                if (bytes.size >= 32) return bytes.take(32).toByteArray()
            } catch (t: Throwable) {
                fallbackFile.delete()
            }
        }
        val raw = ByteArray(32)
        SecureRandom().nextBytes(raw)
        try {
            fallbackFile.writeBytes(raw)
        } catch (ignored: Throwable) {}
        return raw
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val ks = keyStore ?: throw IllegalStateException("KeyStore is null")
        try {
            if (ks.containsAlias(KEY_ALIAS)) {
                val key = ks.getKey(KEY_ALIAS, null) as? SecretKey
                if (key != null) return key
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Stale keystore alias found, removing: ${t.message}")
            try {
                ks.deleteEntry(KEY_ALIAS)
            } catch (ignored: Throwable) {}
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE)
            .setRandomizedEncryptionRequired(true)
            .build()

        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(data)
        return Pair(encrypted, iv)
    }

    private fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), spec)
        return cipher.doFinal(encryptedData)
    }
}
