package com.techsultan.zenithpro.features.auth.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.core.components.OtpTextField
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.navigation.Route
import org.koin.androidx.compose.koinViewModel

@Composable
fun ConfirmEmailScreen(
    email: String,
    onNavigateToLogin: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel()
) {
    val state = authViewModel.signUpState.value

    LaunchedEffect(state.isEmailVerified) {
        if (state.isEmailVerified) {
            onNavigateToLogin()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = Icons.Default.MarkEmailUnread,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "Verify your email",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "We've sent a 6-digit verification code to",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(24.dp))

                OtpTextField(
                    otpText = state.otpCode,
                    otpCount = 6,
                    onOtpTextChange = { text, isComplete ->
                        authViewModel.onOtpChanged(text)
                        if (isComplete) {
                            authViewModel.verifySignupOtp(
                                email = email,
                                token = authViewModel.signUpState.value.otpCode
                            )
                        }
                    },
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    text = "Enter the code above to activate your ZenithPro account.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (state.otpError != null) {
                    Spacer(Modifier.height(12.dp))

                    Text(
                        text = state.otpError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(24.dp))

                ZenithButton(
                    text = if (state.isVerifyingEmail) "Verifying..." else "Verify email",
                    onClick = {
                        authViewModel.verifySignupOtp(
                            email = email,
                            token = authViewModel.signUpState.value.otpCode
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = authViewModel::resendSignupOtp,
                    enabled = !state.isVerifyingEmail
                ) {
                    Text("Didn't receive the code? Resend")
                }

                Spacer(Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        authViewModel.resetSignUpState()
                        onNavigateToLogin()
                    },
                    enabled = !state.isVerifyingEmail
                ) {
                    Text("Back to login")
                }
            }
            if (state.isVerifyingEmail) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}