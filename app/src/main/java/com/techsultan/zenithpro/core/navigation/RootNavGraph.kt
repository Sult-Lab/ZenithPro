package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel

@Composable
fun RootNavGraph(dataPersistentViewModel: DataPersistentViewModel){
    val authState by dataPersistentViewModel.authState.collectAsStateWithLifecycle()

    // Don't render anything until auth is resolved — splash screen handles this window
    if (authState == AuthState.Loading) return

    val initialRoute = remember(authState) {
        if (authState == AuthState.Authenticated) Route.Home else Route.Auth
    }
    val rootBackStack = rememberNavBackStack(initialRoute)

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.Authenticated -> {
                rootBackStack.remove(Route.Auth)
                if (Route.Home !in rootBackStack) rootBackStack.add(Route.Home)
            }
            AuthState.Unauthenticated -> {
                rootBackStack.remove(Route.Home)
                if (Route.Auth !in rootBackStack) rootBackStack.add(Route.Auth)
            }
            AuthState.Loading -> Unit
            else -> {}
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
                    navigateToDashboard = {
                        rootBackStack.remove(Route.Auth)
                        rootBackStack.add(Route.Home)
                    }
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
