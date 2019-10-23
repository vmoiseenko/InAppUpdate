package com.vm.inappupdate.ui.login

import android.annotation.TargetApi
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.Key
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

@TargetApi(Build.VERSION_CODES.M)
class Crypto() {

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }

    private fun getKey(): Key? = keyStore.getKey(KEY_NAME, null)

    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM, KEYSTORE)
        val keyGenParameterSpec =
            KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(BLOCK_MODE)
                .setEncryptionPaddings(PADDING)
                .setUserAuthenticationRequired(true)
                .build()

        keyGenerator.init(keyGenParameterSpec)
        return keyGenerator.generateKey()
    }

    fun getDecryptCipher(iv: ByteArray): Cipher =
        Cipher.getInstance(keyTransformation()).apply { init(Cipher.DECRYPT_MODE, getKey(), IvParameterSpec(iv)) }

    fun getEncryptCipher(): Cipher =
        Cipher.getInstance(keyTransformation()).apply { init(Cipher.ENCRYPT_MODE, createKey()) }

    companion object {
        private const val KEYSTORE = "AndroidKeyStore"
        private const val KEY_NAME = "MY_KEY"
        private const val ALGORITHM = KeyProperties.KEY_ALGORITHM_AES
        private const val BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC
        private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
        private fun keyTransformation() = listOf(ALGORITHM, BLOCK_MODE, PADDING).joinToString(separator = "/")
    }

//    /**
//     * Sets up KeyStore and KeyGenerator
//     */
//    @TargetApi(Build.VERSION_CODES.M)
//    private fun setupKeyStoreAndKeyGenerator() {
//        try {
//            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
//        } catch (e: KeyStoreException) {
//            throw RuntimeException("Failed to get an instance of KeyStore", e)
//        }
//
//        try {
//            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
//        } catch (e: Exception) {
//            when (e) {
//                is NoSuchAlgorithmException,
//                is NoSuchProviderException ->
//                    throw RuntimeException("Failed to get an instance of KeyGenerator", e)
//                else -> throw e
//            }
//        }
//    }
//
//    /**
//     * Sets up default cipher and a non-invalidated cipher
//     */
//    @TargetApi(Build.VERSION_CODES.M)
//    private fun setupCiphers(): Pair<Cipher, Cipher> {
//        val defaultCipher: Cipher
//        val cipherNotInvalidated: Cipher
//        try {
//            val cipherString = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
//            defaultCipher = Cipher.getInstance(cipherString)
//            cipherNotInvalidated = Cipher.getInstance(cipherString)
//        } catch (e: Exception) {
//            when (e) {
//                is NoSuchAlgorithmException,
//                is NoSuchPaddingException ->
//                    throw RuntimeException("Failed to get an instance of Cipher", e)
//                else -> throw e
//            }
//        }
//        return Pair(defaultCipher, cipherNotInvalidated)
//    }
//
//    /**
//     * Initialize the [Cipher] instance with the created key in the [createKey] method.
//     *
//     * @param keyName the key name to init the cipher
//     * @return `true` if initialization succeeded, `false` if the lock screen has been disabled or
//     * reset after key generation, or if a fingerprint was enrolled after key generation.
//     */
//    @TargetApi(Build.VERSION_CODES.M)
//    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
//        try {
//            keyStore.load(null)
//            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(keyName, null) as SecretKey)
//            return true
//        } catch (e: Exception) {
//            when (e) {
//                is KeyPermanentlyInvalidatedException -> return false
//                is KeyStoreException,
//                is CertificateException,
//                is UnrecoverableKeyException,
//                is IOException,
//                is NoSuchAlgorithmException,
//                is InvalidKeyException -> throw RuntimeException("Failed to init Cipher", e)
//                else -> throw e
//            }
//        }
//    }

}