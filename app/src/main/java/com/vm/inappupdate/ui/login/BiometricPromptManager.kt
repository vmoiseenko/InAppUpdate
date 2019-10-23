package com.vm.inappupdate.ui.login

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.Executors
import javax.crypto.Cipher

@RequiresApi(Build.VERSION_CODES.M)
class BiometricPromptManager(
    private val activity: FragmentActivity
) {

    private val crypto = Crypto()

    fun decryptPrompt(
        dataEncrypted: ByteArray,
        initializationVector: ByteArray?,
        failedAction: (errString: CharSequence) -> Unit,
        successAction: (ByteArray) -> Unit
    ) {
        try {
            if (initializationVector != null) {
                val cipher = crypto.getDecryptCipher(initializationVector)
                handleDecrypt(dataEncrypted, cipher, failedAction, successAction)
            } else {
                failedAction("init vector is null!")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Decrypt BiometricPrompt exception", e)
            failedAction(e.stackTrace.toString())
        }
    }

    fun encryptPrompt(
        data: ByteArray,
        failedAction: (errString: CharSequence) -> Unit,
        successAction: (encryptedData: ByteArray, initVector: ByteArray) -> Unit
    ) {
        try {
            val cipher = crypto.getEncryptCipher()
            handleEncrypt(cipher, data, failedAction, successAction)
        } catch (e: Exception) {
            Log.d(TAG, "Encrypt BiometricPrompt exception", e)
            failedAction(e.stackTrace.toString())
        }
    }

    private fun handleDecrypt(
        dataEncrypted: ByteArray,
        cipher: Cipher,
        failedAction: (errString: CharSequence) -> Unit,
        successAction: (ByteArray) -> Unit
    ) {

        val executor = Executors.newSingleThreadExecutor()
        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.cipher?.let { cipher ->
                        val data = cipher.doFinal(dataEncrypted)
                        activity.runOnUiThread { successAction(data) }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.d(TAG, "Authentication error. $errString ($errorCode)")
                    activity.runOnUiThread { failedAction("Authentication error. $errString ($errorCode)") }
                }
            })

        val promptInfo = biometricPromptInfo()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun handleEncrypt(
        cipher: Cipher,
        data: ByteArray,
        failedAction: (errString: CharSequence) -> Unit,
        successAction: (encryptedData: ByteArray, initVector: ByteArray) -> Unit
    ) {

        val executor = Executors.newSingleThreadExecutor()
        val biometricPrompt =
            BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    result.cryptoObject?.cipher?.let { resultCipher ->
                        val iv = resultCipher.iv
                        val encryptedData = resultCipher.doFinal(data)
                        activity.runOnUiThread { successAction(encryptedData, iv) }
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    activity.runOnUiThread { failedAction(errString) }
                }
            })

        val promptInfo = biometricPromptInfo()
        biometricPrompt.authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }

    private fun biometricPromptInfo(): BiometricPrompt.PromptInfo {
        return BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric login")
            .setDescription("Confirm fingerprint to continue")
            .setNegativeButtonText(activity.getString(android.R.string.cancel))
            .build()
    }

    companion object {
        private const val TAG = "BiometricPrompt"
    }
}