package com.vm.inappupdate

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.snackbar.Snackbar
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.InstallState
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.AppUpdateType.IMMEDIATE
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.android.synthetic.main.bottom_sheet.*
import kotlinx.android.synthetic.main.content_main.*


class MainActivity : AppCompatActivity(), InstallStateUpdatedListener {

    private var pressedCount = 0
    private var checkForUpdatesAuto = true
    private var checkForUpdatesManually = false

    private lateinit var sheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var appUpdateManager: AppUpdateManager

    override fun onResume() {
        super.onResume()
        checkForUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sheetBehavior = BottomSheetBehavior.from(findViewById(R.id.bottom_sheet))

        textView.setOnClickListener {
            pressedCount++
            Snackbar.make(it, "Smile clicked $pressedCount times!", Snackbar.LENGTH_SHORT).show()
        }

        appVersionView.text = getString(R.string.app_version, BuildConfig.VERSION_CODE, BuildConfig.VERSION_NAME)
        appVersionView.setOnClickListener {
            checkForUpdatesAuto = true
            checkForUpdatesManually = true
            checkForUpdates()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterAppUpdateManager()
    }

    private fun unregisterAppUpdateManager() {
        appUpdateManager.unregisterListener(this)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(DEBUG, "onActivityResult: requestCode=$resultCode resultCode=$resultCode")
        if (requestCode == REQUEST_CODE_UPDATE) {
            when (requestCode) {
                Activity.RESULT_OK -> Log.d(DEBUG, "Update flow success! Result code: RESULT_OK=$resultCode")
                Activity.RESULT_CANCELED -> Log.d(
                    DEBUG,
                    "Update flow failed/canceled! Result code: RESULT_CANCELED=$resultCode"
                )
                else -> Log.d(DEBUG, "Update flow unknown! Result code: $resultCode")
            }
            checkForUpdatesAuto = false
        }
    }

    private fun checkForUpdates() {

        if (checkForUpdatesAuto.not()) return

        appUpdateManager = AppUpdateManagerFactory.create(this)

        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateManager.registerListener(this)

        Log.d(DEBUG, "UpdateAvailability checkForUpdates")

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->

            Log.d(DEBUG, appUpdateInfo.availableVersionCode().toString())

            when (appUpdateInfo.updateAvailability()) {
                UpdateAvailability.UPDATE_AVAILABLE -> {
                    Log.d(DEBUG, "UpdateAvailability UPDATE_AVAILABLE")

                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)) {

                        Log.d(DEBUG, "UpdateTypeAllowed (AppUpdateType.FLEXIBLE)")

                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            AppUpdateType.IMMEDIATE,
                            // The current activity.
                            this,
                            REQUEST_CODE_UPDATE
                        )
                    }
                }
                UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                    Log.d(DEBUG, "UpdateAvailability DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS")
                    // If an in-app update is already running, resume the update.
                    appUpdateManager.startUpdateFlowForResult(
                        appUpdateInfo,
                        IMMEDIATE,
                        this,
                        REQUEST_CODE_UPDATE
                    )
                }
                UpdateAvailability.UPDATE_NOT_AVAILABLE -> {
                    Log.d(DEBUG, "UpdateAvailability UPDATE_NOT_AVAILABLE")
                    checkForUpdatesAuto = false
                    if (checkForUpdatesManually) {
                        popupSnackbarNoUpdates()
                    }
                }
                UpdateAvailability.UNKNOWN -> Log.d(DEBUG, "UpdateAvailability UNKNOWN")
            }
        }
    }

    override fun onStateUpdate(state: InstallState?) {

        Log.d(DEBUG, state?.toString())

        if (state?.installStatus() == InstallStatus.DOWNLOADED) {
            Log.d(DEBUG, "UpdateAvailability InstallStatus.DOWNLOADED")
            popupForCompleteUpdate()
        }
    }

    private fun popupSnackbarNoUpdates() {
        checkForUpdatesManually = false
        val snackbar =
            Snackbar.make(
                findViewById(R.id.main_container),
                "You are using latest app. There is no available updates!",
                Snackbar.LENGTH_LONG
            )
        snackbar.show()
    }

    private fun popupForCompleteUpdate() {
        sheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
        restart.setOnClickListener { appUpdateManager.completeUpdate() }
        postpone.setOnClickListener { sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED }
    }

    companion object {
        const val DEBUG = "INDEBUG"
        const val REQUEST_CODE_UPDATE = 1
    }
}
