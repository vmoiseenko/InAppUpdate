package com.vm.inappupdate.ui.login

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Base64
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.vm.inappupdate.R
import com.vm.inappupdate.biometrics.FingerprintUiHelper
import kotlinx.android.synthetic.main.activity_login.*


class LoginActivity : AppCompatActivity() {

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var sharedPreferences: SharedPreferences

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login)
        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.fingerprint_bottom_sheet))
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        login.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BiometricPromptManager(this).decryptPrompt(
                    getEncryptedData()!!,
                    getInitializationVector(),
                    failedAction = { showToast("failed to decryptPrompt") },
                    successAction = { password.setText(String(it)) }
                )
            }
        }

        register.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                BiometricPromptManager(this).encryptPrompt(
                    data = password.text.toString().toByteArray(),
                    failedAction = { showToast("failed to encryptPrompt") },
                    successAction = { encryptedData, initVector ->
                        encrypted.text = getString(R.string.encrypted) + " $encryptedData"
                        showToast("encrypted: $encryptedData")
                        saveEncryptedData(encryptedData, initVector)
                    }
                )
            }
        }
    }

    private fun saveEncryptedData(dataEncrypted: ByteArray, initializationVector: ByteArray) {
        sharedPreferences.edit {
            putString(DATA_ENCRYPTED, Base64.encodeToString(dataEncrypted, Base64.DEFAULT))
            putString(
                INITIALIZATION_VECTOR,
                Base64.encodeToString(initializationVector, Base64.DEFAULT)
            )
        }
    }

    private fun getInitializationVector(): ByteArray? {
        val iv = sharedPreferences.getString(INITIALIZATION_VECTOR, null)
        return when {
            iv != null -> Base64.decode(iv, Base64.DEFAULT)
            else -> null
        }
    }

    private fun getEncryptedData(): ByteArray? {
        val iv = sharedPreferences.getString(DATA_ENCRYPTED, null)
        return when {
            iv != null -> Base64.decode(iv, Base64.DEFAULT)
            else -> null
        }
    }

    companion object {
        private const val DATA_ENCRYPTED = "DATA_ENCRYPTED"
        private const val INITIALIZATION_VECTOR = "INITIALIZATION_VECTOR"
    }

//    private fun popupFingerprintAuth() {
//        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
//        cancel.setOnClickListener { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
//
//        fingerprintUiHelper = FingerprintUiHelper(
//            FingerprintManagerCompat.from(this),
//            fingerprintIcon,
//            fingerprintLabel,
//            this
//        )
//    }

    private fun AppCompatActivity.showToast(text: String) {
        Toast.makeText(this, text, Toast.LENGTH_LONG).show()
    }
}