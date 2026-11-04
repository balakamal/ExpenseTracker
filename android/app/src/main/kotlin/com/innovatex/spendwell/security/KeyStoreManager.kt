package com.innovatex.spendwell.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages hardware-backed encryption keys via the Android Keystore.
 * Generates and securely protects the 256-bit SQLCipher database encryption passphrase.
 */
@Singleton
class KeyStoreManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "SpendwellDatabaseMasterKey"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val PASSPHRASE_FILE = "spendwell_db_passphrase.enc"
        private const val IV_FILE = "spendwell_db_passphrase.iv"
        private const val KEY_SIZE = 256
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    /**
     * Retrieves or generates the 256-bit SQLCipher passphrase.
     * The raw passphrase is encrypted with an Android Keystore hardware-backed AES key
     * and persisted locally in private app storage.
     */
    @Synchronized
    fun getOrCreateDatabasePassphrase(): ByteArray {
        val passphraseFile = File(context.filesDir, PASSPHRASE_FILE)
        val ivFile = File(context.filesDir, IV_FILE)

        return if (passphraseFile.exists() && ivFile.exists()) {
            val encryptedBytes = passphraseFile.readBytes()
            val iv = ivFile.readBytes()
            decrypt(encryptedBytes, iv)
        } else {
            // Generate a fresh cryptographically secure 256-bit passphrase
            val rawPassphrase = ByteArray(32)
            SecureRandom().nextBytes(rawPassphrase)

            // Encrypt and persist
            val (encryptedBytes, iv) = encrypt(rawPassphrase)
            passphraseFile.writeBytes(encryptedBytes)
            ivFile.writeBytes(iv)

            rawPassphrase
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
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
            keyGenerator.generateKey()
        }
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry
        return entry.secretKey
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
