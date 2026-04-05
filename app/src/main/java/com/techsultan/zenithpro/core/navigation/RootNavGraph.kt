package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel

@Composable
fun RootNavGraph(dataPersistentViewModel: DataPersistentViewModel){
    val isLoggedIn by dataPersistentViewModel.isLoggedIn.collectAsStateWithLifecycle()

    val initialRoute = if (isLoggedIn) Route.Home else Route.Auth
    val rootBackStack = rememberNavBackStack(initialRoute)


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
