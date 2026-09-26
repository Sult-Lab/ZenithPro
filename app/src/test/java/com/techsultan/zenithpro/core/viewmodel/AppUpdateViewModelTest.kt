package com.techsultan.zenithpro.core.viewmodel

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import app.cash.turbine.test
import com.techsultan.zenithpro.core.manager.AppUpdateManager
import com.techsultan.zenithpro.core.manager.UpdateState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class AppUpdateViewModelTest {

    private val appUpdateManager: AppUpdateManager = mock()
    private val installStatusFlow = MutableStateFlow<UpdateState>(UpdateState.Idle)
    private val launcher: ActivityResultLauncher<IntentSenderRequest> = mock()

    private lateinit var viewModel: AppUpdateViewModel

    @Before
    fun setUp() {
        whenever(appUpdateManager.installStatus).thenReturn(installStatusFlow)
        viewModel = AppUpdateViewModel(appUpdateManager)
    }

    @Test
    fun `updateState emits initial state from AppUpdateManager`() = runTest {
        viewModel.updateState.test {
            assertEquals(UpdateState.Idle, awaitItem())
            ensureAllEventsConsumed()
        }
    }

    @Test
    fun `updateState emits updated state when AppUpdateManager installStatus changes`() = runTest {
        viewModel.updateState.test {
            assertEquals(UpdateState.Idle, awaitItem())

            installStatusFlow.value = UpdateState.Downloaded
            assertEquals(UpdateState.Downloaded, awaitItem())

            installStatusFlow.value = UpdateState.Completed
            assertEquals(UpdateState.Completed, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkForUpdates delegates to AppUpdateManager`() {
        viewModel.checkForUpdates(launcher)
        verify(appUpdateManager).checkForUpdates(launcher)
    }

    @Test
    fun `handleUpdateResult delegates to AppUpdateManager`() {
        val resultCode = Activity.RESULT_OK
        viewModel.handleUpdateResult(resultCode)
        verify(appUpdateManager).handleResult(resultCode)
    }

    @Test
    fun `completeUpdate delegates to AppUpdateManager`() {
        viewModel.completeUpdate()
        verify(appUpdateManager).completeUpdate()
    }

    @Test
    fun `unregisterListener delegates to AppUpdateManager`() {
        viewModel.unregisterListener()
        verify(appUpdateManager).unregisterListener()
    }

    @Test
    fun `checkDownloadedOnResume delegates to AppUpdateManager`() {
        viewModel.checkDownloadedOnResume(launcher)
        verify(appUpdateManager).checkForDownloadedUpdateOnResume(launcher)
    }
}
