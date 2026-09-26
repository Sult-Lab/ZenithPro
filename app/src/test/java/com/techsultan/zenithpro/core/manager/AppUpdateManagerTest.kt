package com.techsultan.zenithpro.core.manager

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager as PlayAppUpdateManager
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.ActivityResult
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class AppUpdateManagerTest {

    private lateinit var context: Context
    private val playAppUpdateManager: PlayAppUpdateManager = mock()
    private val launcher: ActivityResultLauncher<IntentSenderRequest> = mock()
    private val appUpdateInfo: AppUpdateInfo = mock()

    private lateinit var appUpdateManager: AppUpdateManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        appUpdateManager = AppUpdateManager(context, playAppUpdateManager)
    }

    @Test
    fun `handleResult with RESULT_OK updates status to Completed`() = runTest {
        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.handleResult(Activity.RESULT_OK)
            assertEquals(UpdateState.Completed, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleResult with RESULT_CANCELED updates status to Cancelled`() = runTest {
        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.handleResult(Activity.RESULT_CANCELED)
            assertEquals(UpdateState.Cancelled, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `handleResult with RESULT_IN_APP_UPDATE_FAILED updates status to Failed`() = runTest {
        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.handleResult(ActivityResult.RESULT_IN_APP_UPDATE_FAILED)
            assertEquals(UpdateState.Failed, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `completeUpdate calls playAppUpdateManager completeUpdate`() {
        appUpdateManager.completeUpdate()
        verify(playAppUpdateManager).completeUpdate()
    }

    @Test
    fun `checkForUpdates when IMMEDIATE update available triggers immediate update flow`() = runTest {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forResult(appUpdateInfo))
        whenever(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(true)
        whenever(appUpdateInfo.clientVersionStalenessDays()).thenReturn(5)
        whenever(appUpdateInfo.updatePriority()).thenReturn(5)

        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.checkForUpdates(launcher)
            assertEquals(UpdateState.Checking, awaitItem())

            ShadowLooper.idleMainLooper()

            val availableState = awaitItem() as UpdateState.UpdateAvailable
            assertEquals(true, availableState.isImmediate)
            assertEquals(5, availableState.stalenessDays)
            assertEquals(5, availableState.priority)

            verify(playAppUpdateManager).startUpdateFlowForResult(
                eq(appUpdateInfo),
                eq(launcher),
                any()
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkForUpdates when FLEXIBLE update available triggers flexible update flow and registers listener`() = runTest {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forResult(appUpdateInfo))
        whenever(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(false)
        whenever(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)).thenReturn(true)
        whenever(appUpdateInfo.clientVersionStalenessDays()).thenReturn(2)
        whenever(appUpdateInfo.updatePriority()).thenReturn(1)

        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.checkForUpdates(launcher)
            assertEquals(UpdateState.Checking, awaitItem())

            ShadowLooper.idleMainLooper()

            val availableState = awaitItem() as UpdateState.UpdateAvailable
            assertEquals(false, availableState.isImmediate)
            assertEquals(2, availableState.stalenessDays)
            assertEquals(1, availableState.priority)

            verify(playAppUpdateManager).startUpdateFlowForResult(
                eq(appUpdateInfo),
                eq(launcher),
                any()
            )
            verify(playAppUpdateManager).registerListener(any())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkForUpdates when status is DOWNLOADED updates status to Downloaded`() = runTest {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forResult(appUpdateInfo))
        whenever(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UNKNOWN)
        whenever(appUpdateInfo.installStatus()).thenReturn(InstallStatus.DOWNLOADED)

        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.checkForUpdates(launcher)
            assertEquals(UpdateState.Checking, awaitItem())

            ShadowLooper.idleMainLooper()

            assertEquals(UpdateState.Downloaded, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkForUpdates when task fails updates status to Failed`() = runTest {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forException(RuntimeException("Network Error")))

        appUpdateManager.installStatus.test {
            assertEquals(UpdateState.Idle, awaitItem())

            appUpdateManager.checkForUpdates(launcher)
            assertEquals(UpdateState.Checking, awaitItem())

            ShadowLooper.idleMainLooper()

            assertEquals(UpdateState.Failed, awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `checkForDownloadedUpdateOnResume when update in progress resumes update flow`() {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forResult(appUpdateInfo))
        whenever(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS)

        appUpdateManager.checkForDownloadedUpdateOnResume(launcher)
        ShadowLooper.idleMainLooper()

        verify(playAppUpdateManager).startUpdateFlowForResult(
            eq(appUpdateInfo),
            eq(launcher),
            any()
        )
    }

    @Test
    fun `unregisterListener removes listener from playAppUpdateManager when registered`() {
        whenever(playAppUpdateManager.appUpdateInfo).thenReturn(Tasks.forResult(appUpdateInfo))
        whenever(appUpdateInfo.updateAvailability()).thenReturn(UpdateAvailability.UPDATE_AVAILABLE)
        whenever(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)).thenReturn(false)
        whenever(appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)).thenReturn(true)

        appUpdateManager.checkForUpdates(launcher)
        ShadowLooper.idleMainLooper()

        appUpdateManager.unregisterListener()

        verify(playAppUpdateManager).unregisterListener(any<InstallStateUpdatedListener>())
    }
}
