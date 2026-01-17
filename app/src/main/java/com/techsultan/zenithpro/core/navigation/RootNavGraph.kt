package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay

@Composable
fun RootNavGraph(){

    val rootBackStack = rememberNavBackStack(Route.Auth)

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