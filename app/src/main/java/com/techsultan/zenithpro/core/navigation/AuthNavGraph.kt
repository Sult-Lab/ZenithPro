package com.techsultan.zenithpro.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.techsultan.zenithpro.core.util.AuthState
import com.techsultan.zenithpro.core.viewmodel.DataPersistentViewModel
import com.techsultan.zenithpro.features.auth.presentation.ChangePasswordScreen
import com.techsultan.zenithpro.features.auth.presentation.SignInScreen
import com.techsultan.zenithpro.features.auth.presentation.SignUpScreen

@Composable
fun AuthGraph(
    dataPersistentViewModel: DataPersistentViewModel,
    navigateToDashboard: () -> Unit,
){

    val authState by dataPersistentViewModel.authState.collectAsStateWithLifecycle()
    val initialRoute = remember {
        if (authState == AuthState.MustChangePassword) {
            Route.Auth.ChangePassword(mode = Route.PasswordChangeMode.FORCED)
        } else {
            Route.Auth.SignIn
        }
    }
    val authBackStack = rememberNavBackStack(initialRoute)

    LaunchedEffect(authState) {
        if (authState == AuthState.MustChangePassword) {
            if (authBackStack.lastOrNull() !is Route.Auth.ChangePassword) {
                authBackStack.add(Route.Auth.ChangePassword(mode = Route.PasswordChangeMode.FORCED))
            }
        }
    }

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
                    onCreateAccountClick = { authBackStack.add(Route.Auth.SignUp) },
                    onMustChangePassword = { 
                        authBackStack.add(Route.Auth.ChangePassword(mode = Route.PasswordChangeMode.FORCED)) 
                    },
                    onForgotPasswordClick = { email ->
                        authBackStack.add(Route.Auth.ChangePassword(
                            mode = Route.PasswordChangeMode.FORGOT,
                            email = email
                        ))
                    }
                )
            }
            entry<Route.Auth.SignUp> {
                SignUpScreen(
                    onCreateAccountSuccess = navigateToDashboard,
                    onLoginClick = { authBackStack.add(Route.Auth.SignIn) },
                )
            }
            entry<Route.Auth.ChangePassword> { route ->
                ChangePasswordScreen(
                    mode = route.mode,
                    initialEmail = route.email,
                    onChanged = navigateToDashboard,
                    onBack = { authBackStack.remove(route) }
                )
            }
        }
    )
}