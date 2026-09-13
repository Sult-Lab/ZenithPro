package com.techsultan.zenithpro.core.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel

/**
 * The root navigation graph of the application.
 * Handles the top-level authentication flow and deep linking.
 *
 * @param dataPersistentViewModel The view model providing the current authentication state.
 * @param deepLinkUri The URI of a deep link to be handled, if any.
 * @param onDeepLinkConsumed Callback to be invoked after the deep link has been processed.
 */
@Composable
fun RootNavGraph(
    dataPersistentViewModel: DataPersistentViewModel,
    deepLinkUri: Uri? = null,
    onDeepLinkConsumed: () -> Unit = {}
){
    val authState by dataPersistentViewModel.authState.collectAsStateWithLifecycle()

    // Don't render anything until auth is resolved — splash screen handles this window
    if (authState == AuthState.Loading) return

    val initialRoute = remember(authState) {
        if (authState == AuthState.Authenticated) Route.Home else Route.Auth
    }
    val rootBackStack = rememberNavBackStack(initialRoute)

    var authDeepLink by remember { mutableStateOf<Route?>(null) }
    var isHandlingDeepLink by remember { mutableStateOf(false) }

    // Handle authentication state changes
    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> {
                // Don't remove Auth if we are in the middle of a deep link flow (e.g. password recovery)
                if (!isHandlingDeepLink) {
                    rootBackStack.remove(Route.Auth)
                    if (Route.Home !in rootBackStack) rootBackStack.add(Route.Home)
                }
            }
            AuthState.Unauthenticated, AuthState.MustChangePassword -> {
                rootBackStack.remove(Route.Home)
                if (Route.Auth !in rootBackStack) rootBackStack.add(Route.Auth)
            }
            AuthState.Loading -> Unit
        }
    }

    // Handle deep links
    LaunchedEffect(deepLinkUri, authState) {
        val uri = deepLinkUri ?: return@LaunchedEffect

        // Standard deep link handling logic
        when {
            // Handle website URLs: https://www.zenithpro.name.ng or https://zenithpro.name.ng
            uri.scheme == "https" && (uri.host == "www.zenithpro.name.ng" || uri.host == "zenithpro.name.ng") -> {
                if (authState == AuthState.Authenticated) {
                    if (Route.Home !in rootBackStack) {
                        rootBackStack.add(Route.Home)
                    }
                }
                onDeepLinkConsumed()
            }

            // Handle custom scheme: zenithpro://auth/confirm
            uri.scheme == "zenithpro" && uri.host == "auth" && uri.path?.startsWith("/confirm") == true -> {
                val token = uri.getQueryParameter("token")
                
                if (!token.isNullOrEmpty()) {
                    isHandlingDeepLink = true
                    authDeepLink = Route.Auth.EmailConfirmed(token)

                    if (Route.Auth !in rootBackStack) {
                        rootBackStack.add(Route.Auth)
                    }
                }
                onDeepLinkConsumed()
            }
        }
    }


    NavDisplay(
        backStack = rootBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Route.Auth> {
                AuthGraph(
                    dataPersistentViewModel = dataPersistentViewModel,
                    navigateToDashboard = {
                        isHandlingDeepLink = false
                        rootBackStack.remove(Route.Auth)
                        rootBackStack.add(Route.Home)
                    },
                    initialDeepLink = authDeepLink,
                    onDeepLinkConsumed = { authDeepLink = null }
                )
            }
            entry<Route.Home> {
                MainNavGraph(
                    dataPersistentViewModel = dataPersistentViewModel
                )
            }
        }
    )
}
