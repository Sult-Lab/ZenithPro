package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.features.auth.presentation.SignInScreen
import com.techsultan.zenithpro.features.auth.presentation.SignUpScreen

@Composable
fun AuthGraph(
    navigateToDashboard: () -> Unit,
){

    val authBackStack = rememberNavBackStack(Route.Auth.SignIn)

    NavDisplay(
        modifier = Modifier,
        backStack = authBackStack,
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator()
        ),
        entryProvider = entryProvider {
            entry<Route.Auth.SignIn> {
                SignInScreen(
                    onLoginClick = navigateToDashboard,
                    onCreateAccountClick = { authBackStack.add(Route.Auth.SignUp) }
                )
            }
            entry<Route.Auth.SignUp> {
                SignUpScreen(
                    onCreateAccount = navigateToDashboard,
                    onLoginClick = { authBackStack.add(Route.Auth.SignIn) }
                )
            }
        }
    )
}