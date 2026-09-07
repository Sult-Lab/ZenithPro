package com.techsultan.zenithpro.features.auth.presentation

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.techsultan.zenithpro.R
import com.techsultan.zenithpro.core.components.ZenithButton
import com.techsultan.zenithpro.core.components.ZenithLogoSection
import com.techsultan.zenithpro.core.components.ZenithPhoneNumberField
import com.techsultan.zenithpro.core.components.checkAndRequestStoragePermission
import com.techsultan.zenithpro.core.components.rememberStoragePermissionLauncher
import com.techsultan.zenithpro.features.auth.data.remote.SignUpRequest
import org.koin.androidx.compose.koinViewModel

@Composable
fun SignUpScreen(
    viewModel: AuthViewModel = koinViewModel(),
    onCreateAccountSuccess: () -> Unit,
    onLoginClick: () -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var businessPhone by remember { mutableStateOf("") }
    var businessAddress by remember { mutableStateOf("") }
    var businessLogoUri by remember { mutableStateOf<Uri?>(null) }
    var adminFirstName by remember { mutableStateOf("") }
    var adminLastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    val state = viewModel.signUpState.value
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { businessLogoUri = it }
    }

    val storagePermissionLauncher = rememberStoragePermissionLauncher(
        onPermissionGranted = {
            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
    )

    LaunchedEffect(key1 = state.isSuccess) {
        if (state.isSuccess) {
            onCreateAccountSuccess()
        }
    }

    LaunchedEffect(key1 = state.error) {
        state.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Scaffold() { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(32.dp))

                ZenithLogoSection(
                    logoUri = businessLogoUri,
                    onPick = {
                        checkAndRequestStoragePermission(context, storagePermissionLauncher) {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    onRemove = { businessLogoUri = null }
                )

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Register Your Business",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.Black
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = buildAnnotatedString {
                        append("Join thousands of Nigerian businesses managing inventory and sales in ")
                        withStyle(
                            style = SpanStyle(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append("Zenith Pro")
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                SignUpTextField(
                    label = "BUSINESS NAME",
                    value = businessName,
                    onValueChange = { businessName = it },
                    placeholder = "e.g. Ade's Electronics",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)

                )

                Spacer(modifier = Modifier.height(16.dp))

                ZenithPhoneNumberField(
                    label = "BUSINESS PHONE NUMBER",
                    value = businessPhone,
                    onValueChange = { businessPhone = it },
                    placeholder = "800 000 0000"
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "BUSINESS PHYSICAL ADDRESS",
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    placeholder = "e.g. 123 Main St, Lagos",
                    singleLine = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "ADMIN FIRST NAME",
                    value = adminFirstName,
                    onValueChange = { adminFirstName = it },
                    placeholder = "Enter your first name",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "ADMIN LAST NAME",
                    value = adminLastName,
                    onValueChange = { adminLastName = it },
                    placeholder = "Enter your last name",
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "ADMIN / BUSINESS EMAIL ADDRESS",
                    value = email,
                    onValueChange = { email = it },
                    placeholder = "admin@business.com",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "CREATE PASSWORD",
                    value = password,
                    onValueChange = { password = it },
                    placeholder = "Min. 8 characters",
                    isPassword = true,
                    passwordVisible = passwordVisible,
                    onPasswordToggle = { passwordVisible = !passwordVisible }
                )

                Spacer(modifier = Modifier.height(16.dp))

                SignUpTextField(
                    label = "CONFIRM PASSWORD",
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    placeholder = "Min. 8 characters",
                    isPassword = true,
                    passwordVisible = confirmPasswordVisible,
                    onPasswordToggle = { confirmPasswordVisible = !confirmPasswordVisible }
                )

                Spacer(modifier = Modifier.height(24.dp))

                val annotatedString = buildAnnotatedString {
                    append("By clicking Create Account, you agree to our ")
                    withLink(LinkAnnotation.Url("https://zenithpro-web.vercel.app/terms")) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)) {
                            append("Terms")
                        }
                    }
                    append(" and ")
                    withLink(LinkAnnotation.Url("https://zenithpro-web.vercel.app/privacy")) {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)) {
                            append("privacy policy")
                        }
                    }
                    append("\nyour default currency will be the ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.SemiBold)) {
                        append("Nigerian Naira (N).")
                    }
                }

                Text(
                    text = annotatedString,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(32.dp))

                ZenithButton(
                    text = "Create Account",
                    onClick = {
                        viewModel.signUp(
                            request = SignUpRequest(
                                email = email.trim(),
                                password = password.trim(),
                                confirmPassword = confirmPassword.trim(),
                                adminFirstName = adminFirstName,
                                adminLastName = adminLastName,
                                businessName = businessName,
                                businessPhone = businessPhone.trim(),
                                businessAddress = businessAddress.trim(),
                                acceptTerms = true
                            ),
                            logoUri = businessLogoUri
                        )
                    },
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Already have an account? ",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Text(
                        text = "Log In",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { onLoginClick() }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (state.isLoading) {
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

@Composable
fun SignUpTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onPasswordToggle: () -> Unit = {},
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Color.LightGray
                )
            },
            visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
            trailingIcon = if (isPassword) {
                {
                    IconButton(onClick = onPasswordToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }
            } else null,
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Color.LightGray,
                unfocusedBorderColor = Color.LightGray
            ),
            keyboardOptions = keyboardOptions,
            singleLine = singleLine
        )
    }
}

@Preview
@Composable
fun SignUpScreenPreview() {
    // SignUpScreen(onCreateAccountSuccess = {}, onLoginClick = {})
}