package com.techsultan.zenithpro.core.manager

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppUpdateManager(
    private val context: Context,
    private val updateManager: com.google.android.play.core.appupdate.AppUpdateManager = AppUpdateManagerFactory.create(context)
) {

    private val _installStatus = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val installStatus: StateFlow<UpdateState> = _installStatus

    private var installStateListener: InstallStateUpdatedListener? = null
    private var listenerRegistered = false

    fun handleResult(resultCode: Int) {
        when (resultCode) {
            Activity.RESULT_OK -> { _installStatus.value = UpdateState.Completed }
            Activity.RESULT_CANCELED -> { _installStatus.value = UpdateState.Cancelled }
            ActivityResult.RESULT_IN_APP_UPDATE_FAILED -> { _installStatus.value = UpdateState.Failed }
        }
    }

    fun checkForUpdates(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        _installStatus.value = UpdateState.Checking

        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            val staleness = info.clientVersionStalenessDays()
            val priority = info.updatePriority()
            val isUpdateAvailable = info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE

            if (isUpdateAvailable) {
                when {
                    (info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) && priority >= 4) -> {
                        _installStatus.value = UpdateState.UpdateAvailable(isImmediate = true, stalenessDays = staleness, priority = priority)
                        startUpdate(info, launcher, AppUpdateType.IMMEDIATE)
                    }

                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                        _installStatus.value = UpdateState.UpdateAvailable(isImmediate = false, stalenessDays = staleness, priority = priority)
                        startUpdate(info, launcher, AppUpdateType.FLEXIBLE)
                        observeFlexibleUpdates()
                    }
                    else -> { _installStatus.value = UpdateState.Idle }
                }
            } else if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _installStatus.value = UpdateState.Downloaded
            }
        }.addOnFailureListener {
            _installStatus.value = UpdateState.Failed
            Log.d("In-App-Update-Log", "failed message ${it.message}")
        }
    }

    private fun startUpdate(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>, type: Int) {
        val options = AppUpdateOptions.newBuilder(type)
            .setAllowAssetPackDeletion(true)
            .build()
        try {
            updateManager.startUpdateFlowForResult(info, launcher, options)
        } catch (e: Exception) {
            e.printStackTrace()
            _installStatus.value = UpdateState.Failed
        }
    }


    private fun observeFlexibleUpdates() {
        if (listenerRegistered) return
        installStateListener = InstallStateUpdatedListener { state ->
            when (state.installStatus()) {
                InstallStatus.DOWNLOADED -> _installStatus.value = UpdateState.Downloaded
                InstallStatus.INSTALLING -> _installStatus.value = UpdateState.Installing
                InstallStatus.INSTALLED -> _installStatus.value = UpdateState.Completed
                InstallStatus.DOWNLOADING -> {
                    _installStatus.value = UpdateState.Downloading(state.bytesDownloaded(), state.totalBytesToDownload())
                }
            }
        }
        updateManager.registerListener(installStateListener!!)
        listenerRegistered = true
    }

    fun checkForDownloadedUpdateOnResume(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        updateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                updateManager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build())
            }
            if (info.installStatus() == InstallStatus.DOWNLOADED) {
                _installStatus.value = UpdateState.Downloaded
            }
        }
    }

    fun completeUpdate() {
        updateManager.completeUpdate()
    }

    fun unregisterListener() {
        installStateListener?.let {
            updateManager.unregisterListener(it)
            listenerRegistered = false
            installStateListener = null
        }
    }

}

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    object Downloaded : UpdateState()
    object Installing : UpdateState()
    object Completed : UpdateState()
    object Failed : UpdateState()
    object Cancelled : UpdateState()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : UpdateState()
    data class UpdateAvailable(val isImmediate: Boolean, val stalenessDays: Int?, val priority: Int) : UpdateState()
}