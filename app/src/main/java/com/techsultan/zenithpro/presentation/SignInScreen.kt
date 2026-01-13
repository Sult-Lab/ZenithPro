package com.techsultan.zenithpro.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.techsultan.zenithpro.presentation.components.ZenithButton

@Composable
fun SignInScreen(
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(40.dp))

            // Logo Section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Welcome Back",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Text(
                text = "Please sign in to your account",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Form Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Email Address",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("you@example.com") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Password",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your password") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline
                        )
                    },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = icon, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.clickable { /* Handle Forgot Password */ }
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            ZenithButton(
                text = "Sign In",
                onClick = onLoginClick,
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Sign Up Row
            val annotatedString = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                    append("Don't have an account? ")
                }
                withStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                ) {
                    append("Sign Up")
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.clickable { onCreateAccountClick() }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Preview
@Composable
fun SignInScreen(){
    SignInScreen(onLoginClick = {}, onCreateAccountClick = {})
}