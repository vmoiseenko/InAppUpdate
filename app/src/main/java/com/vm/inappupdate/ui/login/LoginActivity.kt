package com.vm.inappupdate.ui.login

import android.annotation.TargetApi
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vm.inappupdate.R
import com.vm.inappupdate.biometrics.FingerprintUiHelper
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.fingerprint_bottom_sheet.*
import java.io.IOException
import java.security.*
import java.security.cert.CertificateException
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.NoSuchPaddingException
import javax.crypto.SecretKey


class LoginActivity : AppCompatActivity(), FingerprintUiHelper.Callback {

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var fingerprintUiHelper: FingerprintUiHelper

    private lateinit var keyStore: KeyStore
    private lateinit var keyGenerator: KeyGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.fingerprint_bottom_sheet))
        login.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BiometricPromptManager(this).decryptPrompt(
                    failedAction = { showToast("failed to encryptPrompt") },
                    successAction = { password.setText(String(it))}
                )
            }
        }

        register.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BiometricPromptManager(this).encryptPrompt(
                    data = password.text.toString().toByteArray(),
                    failedAction = { showToast("failed to encryptPrompt") },
                    successAction = {
                        showToast("encrypted: $it")
                    }
                )
            }
        }
    }

    private fun popupFingerprintAuth() {
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        cancel.setOnClickListener { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }

        fingerprintUiHelper = FingerprintUiHelper(
            FingerprintManagerCompat.from(this),
            fingerprintIcon,
            fingerprintLabel,
            this
        )

    }

    private fun biometricsPrompt(){
        val activity: FragmentActivity = this
        val executor = Executors.newSingleThreadExecutor()
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    Log.d("TEST","BiometricPrompt.ERROR_NEGATIVE_BUTTON")
                } else {
                    Log.d("TEST","unrecoverable error has been encountered and the operation is complete")
                }
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                Log.d("TEST","biometric is recognized")
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Log.d("TEST","biometric is valid but not recognized.")
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Sign in")
            .setDescription("Set the description to display")
            .setNegativeButtonText("Cancel")
            .build()

        biometricPrompt.authenticate(promptInfo)

    }

    /**
     * Sets up KeyStore and KeyGenerator
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun setupKeyStoreAndKeyGenerator() {
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        } catch (e: KeyStoreException) {
            throw RuntimeException("Failed to get an instance of KeyStore", e)
        }

        try {
            keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchProviderException ->
                    throw RuntimeException("Failed to get an instance of KeyGenerator", e)
                else -> throw e
            }
        }
    }

    /**
     * Sets up default cipher and a non-invalidated cipher
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun setupCiphers(): Pair<Cipher, Cipher> {
        val defaultCipher: Cipher
        val cipherNotInvalidated: Cipher
        try {
            val cipherString = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_CBC}/${KeyProperties.ENCRYPTION_PADDING_PKCS7}"
            defaultCipher = Cipher.getInstance(cipherString)
            cipherNotInvalidated = Cipher.getInstance(cipherString)
        } catch (e: Exception) {
            when (e) {
                is NoSuchAlgorithmException,
                is NoSuchPaddingException ->
                    throw RuntimeException("Failed to get an instance of Cipher", e)
                else -> throw e
            }
        }
        return Pair(defaultCipher, cipherNotInvalidated)
    }

    /**
     * Initialize the [Cipher] instance with the created key in the [createKey] method.
     *
     * @param keyName the key name to init the cipher
     * @return `true` if initialization succeeded, `false` if the lock screen has been disabled or
     * reset after key generation, or if a fingerprint was enrolled after key generation.
     */
    @TargetApi(Build.VERSION_CODES.M)
    private fun initCipher(cipher: Cipher, keyName: String): Boolean {
        try {
            keyStore.load(null)
            cipher.init(Cipher.ENCRYPT_MODE, keyStore.getKey(keyName, null) as SecretKey)
            return true
        } catch (e: Exception) {
            when (e) {
                is KeyPermanentlyInvalidatedException -> return false
                is KeyStoreException,
                is CertificateException,
                is UnrecoverableKeyException,
                is IOException,
                is NoSuchAlgorithmException,
                is InvalidKeyException -> throw RuntimeException("Failed to init Cipher", e)
                else -> throw e
            }
        }
    }

    override fun onAuthenticated() {
    }

    override fun onError() {
        Log.d("TEST","onError()")
    }

    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val ENCRYPTED_VALUE = "EncryptedValue"
    }

    fun AppCompatActivity.showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}


/**
 * Extension function to simplify setting an afterTextChanged action to EditText components.
 */
fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}
