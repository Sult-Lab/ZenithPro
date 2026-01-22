package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel

@Composable
fun RootNavGraph(viewModel: DataPersistentViewModel){
    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()
    val rootBackStack = rememberNavBackStack(Route.Auth)

    LaunchedEffect(isLoggedIn) {
        when (isLoggedIn) {
            true -> {
                rootBackStack.remove(Route.Auth)
                rootBackStack.add(Route.Home)
            }
            false -> {
                rootBackStack.remove(Route.Home)
                rootBackStack.add(Route.Auth)
            }
            null -> Unit
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
                MainNavGraph()
            }
        }
    )
}
