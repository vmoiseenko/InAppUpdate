package com.vm.inappupdate.biometrics

import android.content.Context
import androidx.core.hardware.fingerprint.FingerprintManagerCompat
import android.content.Context.KEYGUARD_SERVICE
import android.app.KeyguardManager
import androidx.annotation.NonNull
import android.content.pm.PackageManager
import android.Manifest.permission
import android.Manifest.permission.USE_FINGERPRINT
import androidx.core.app.ActivityCompat
import android.os.Build





class BiometricHelper {

    fun isHardwareSupported(context: Context): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.isHardwareDetected
    }

    fun isFingerprintAvailable(context: Context): Boolean {
        val fingerprintManager = FingerprintManagerCompat.from(context)
        return fingerprintManager.hasEnrolledFingerprints()
    }

    fun checkSensorState(context: Context): SensorState {
        if (checkFingerprintCompatibility(context)) {
            val keyguardManager = context.getSystemService(KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isKeyguardSecure) {
                return SensorState.NOT_BLOCKED
            }
            val fingerprintManager = FingerprintManagerCompat.from(context)
            return if (!fingerprintManager.hasEnrolledFingerprints()) {
                SensorState.NO_FINGERPRINTS
            } else SensorState.READY
        } else {
            return SensorState.NOT_SUPPORTED
        }
    }

    fun checkFingerprintCompatibility(context: Context): Boolean {
        return FingerprintManagerCompat.from(context).isHardwareDetected
    }

}