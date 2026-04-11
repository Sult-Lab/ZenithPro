package com.techsultan.zenithpro.features.auth.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    isForcedChange: Boolean = false,
    viewModel: ChangePasswordViewModel = koinViewModel(),
    onChanged: () -> Unit,
    onSkip: (() -> Unit)? = null,
    onBack: () -> Unit,
) {
    val state        by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHost  = remember { SnackbarHostState() }
    var showPassword  by remember { mutableStateOf(false) }
    var showConfirm   by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ChangePasswordViewModel.ChangePasswordEvent.Changed -> {
                    snackbarHost.showSnackbar("Password changed successfully")
                    onChanged()
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHost) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isForcedChange) "Set new password"
                        else "Change password"
                    )
                },
                navigationIcon = {
                    if (!isForcedChange) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Default.ArrowBack, null)
                        }
                    }
                },
                actions = {
                    // Allow skipping only when not forced and onSkip provided
                    if (!isForcedChange && onSkip != null) {
                        TextButton(onClick = onSkip) {
                            Text("Skip")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isForcedChange) {
                Surface(
                    shape  = RoundedCornerShape(12.dp),
                    color  = Color(0xFFF57C00).copy(alpha = 0.08f),
                    border = BorderStroke(1.dp, Color(0xFFF57C00).copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint     = Color(0xFFF57C00),
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                "Password change required",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF57C00)
                            )
                            Text(
                                "Your administrator set a temporary password. " +
                                        "Please create a new one to continue.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFF57C00).copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val strength = passwordStrength(state.password)

            OutlinedTextField(
                value = state.password,
                onValueChange = viewModel::onPasswordChanged,
                label = { Text("New password") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.passwordError != null,
                supportingText = state.passwordError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                },
                visualTransformation = if (showPassword)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            if (showPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            // Strength bar — only show when user has typed something
            if (state.password.isNotEmpty()) {
                PasswordStrengthBar(strength = strength)
            }

            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChanged,
                label = { Text("Confirm new password") },
                modifier = Modifier.fillMaxWidth(),
                isError = state.confirmError != null,
                supportingText = state.confirmError?.let {
                    { Text(it, color = MaterialTheme.colorScheme.error) }
                } ?: if (
                    state.confirmPassword.isNotEmpty() &&
                    state.password == state.confirmPassword
                ) {
                    {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint     = Color(0xFF388E3C),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                "Passwords match",
                                color = Color(0xFF388E3C),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                } else null,
                visualTransformation = if (showConfirm)
                    VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showConfirm = !showConfirm }) {
                        Icon(
                            if (showConfirm) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = null
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction    = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { viewModel.submit() }
                ),
                singleLine = true,
                shape      = RoundedCornerShape(12.dp)
            )

            // ── Password requirements checklist ───────────────────
            if (state.password.isNotEmpty()) {
                PasswordRequirements(password = state.password)
            }

            // ── Error banner ──────────────────────────────────────
            state.error?.let { error ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier          = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick  = viewModel::submit,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled  = !state.isLoading && strength != PasswordStrength.WEAK,
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00C853)
                )
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(20.dp),
                        color       = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Default.LockReset,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Change password",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

enum class PasswordStrength { WEAK, FAIR, STRONG, VERY_STRONG }

fun passwordStrength(password: String): PasswordStrength {
    if (password.length < 6) return PasswordStrength.WEAK
    var score = 0
    if (password.length >= 8)                           score++
    if (password.length >= 12)                          score++
    if (password.any { it.isUpperCase() })              score++
    if (password.any { it.isLowerCase() })              score++
    if (password.any { it.isDigit() })                  score++
    if (password.any { !it.isLetterOrDigit() })         score++
    return when {
        score <= 2 -> PasswordStrength.FAIR
        score <= 4 -> PasswordStrength.STRONG
        else       -> PasswordStrength.VERY_STRONG
    }
}

@Composable
private fun PasswordStrengthBar(strength: PasswordStrength) {
    val (label, color, fraction) = when (strength) {
        PasswordStrength.WEAK       -> Triple("Weak",        Color(0xFFD32F2F), 0.25f)
        PasswordStrength.FAIR       -> Triple("Fair",        Color(0xFFF57C00), 0.5f)
        PasswordStrength.STRONG     -> Triple("Strong",      Color(0xFF388E3C), 0.75f)
        PasswordStrength.VERY_STRONG -> Triple("Very strong", Color(0xFF1976D2), 1.0f)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LinearProgressIndicator(
            progress   = { fraction },
            modifier   = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color      = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Text(
            "Strength: $label",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun PasswordRequirements(password: String) {
    val requirements = listOf(
        "At least 6 characters"          to (password.length >= 6),
        "At least 8 characters"          to (password.length >= 8),
        "Contains uppercase letter"      to password.any { it.isUpperCase() },
        "Contains lowercase letter"      to password.any { it.isLowerCase() },
        "Contains number"                to password.any { it.isDigit() },
        "Contains special character"     to password.any { !it.isLetterOrDigit() }
    )

    Surface(
        shape    = RoundedCornerShape(10.dp),
        color    = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier  = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Password requirements",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            requirements.forEach { (label, met) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector  = if (met) Icons.Default.CheckCircle
                        else Icons.Default.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (met) Color(0xFF388E3C)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                            .copy(alpha = 0.5f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        label,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (met) Color(0xFF388E3C)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}