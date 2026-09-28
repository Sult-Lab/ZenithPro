package com.techsultan.zenithpro.core.viewmodel

import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.lifecycle.ViewModel
import com.techsultan.zenithpro.core.manager.AppUpdateManager
import com.techsultan.zenithpro.core.manager.UpdateState
import kotlinx.coroutines.flow.StateFlow

class AppUpdateViewModel(
    private val appUpdateManager: AppUpdateManager
) : ViewModel() {

    val updateState: StateFlow<UpdateState> = appUpdateManager.installStatus

    fun checkForUpdates(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        appUpdateManager.checkForUpdates(launcher)
    }

    fun handleUpdateResult(resultCode: Int) = appUpdateManager.handleResult(resultCode)
    fun completeUpdate() = appUpdateManager.completeUpdate()
    fun unregisterListener() = appUpdateManager.unregisterListener()
    fun checkDownloadedOnResume(launcher: ActivityResultLauncher<IntentSenderRequest>) =
        appUpdateManager.checkForDownloadedUpdateOnResume(launcher)

}