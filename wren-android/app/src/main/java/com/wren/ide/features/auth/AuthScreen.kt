package com.wren.ide.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.LoginResponse
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun AuthScreen(
    sessionManager: SessionManager,
    onAuthSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var navigateToWorkspace by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Safe navigation via LaunchedEffect — prevents crash from calling
    // onAuthSuccess() inside a coroutine's withContext block which can
    // trigger recomposition on a cancelled scope.
    LaunchedEffect(navigateToWorkspace) {
        if (navigateToWorkspace) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryObsidian),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- Logo mark ---
            Text(
                text = "W",
                color = ElectricCyan,
                fontSize = 52.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-2).sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // --- Brand name ---
            Text(
                text = "Wren",
                color = TextLight,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // --- Subtitle ---
            Text(
                text = "Your intelligent development environment",
                color = TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(40.dp))

            // --- Email field ---
            MinimalTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = "Email address",
                isPassword = false
            )

            Spacer(modifier = Modifier.height(20.dp))

            // --- Password field ---
            MinimalTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = "Password",
                isPassword = true
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Error message ---
            AnimatedVisibility(
                visible = errorMessage != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            // --- Continue button ---
            Button(
                onClick = {
                    try {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please fill in all fields."
                            return@Button
                        }
                        isLoading = true
                        errorMessage = null

                        scope.launch(Dispatchers.IO) {
                            try {
                                val endpoint = if (isRegisterMode) "/auth/register" else "/auth/login"
                                val requestBody = mapOf("email" to email, "password" to password)
                                val response = NetworkClient.post(endpoint, requestBody)

                                val responseBodyStr = response.body?.string()
                                if (response.isSuccessful && responseBodyStr != null) {
                                    val loginData = Gson().fromJson(
                                        responseBodyStr,
                                        LoginResponse::class.java
                                    )

                                    // Save session details
                                    sessionManager.jwtToken = loginData.token
                                    sessionManager.userEmail = loginData.user.email
                                    sessionManager.userRole = loginData.user.role
                                    sessionManager.userTier = loginData.user.tier
                                    sessionManager.userCredits = loginData.user.balance

                                    // Update static network helper header
                                    NetworkClient.setAuthToken(loginData.token)

                                    // Signal navigation on the main thread via state,
                                    // NOT by calling onAuthSuccess() directly here.
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        navigateToWorkspace = true
                                    }
                                } else {
                                    val errMap = try {
                                        Gson().fromJson(responseBodyStr, Map::class.java)
                                    } catch (_: Exception) {
                                        null
                                    }
                                    val msg = errMap?.get("error") as? String
                                        ?: "Authentication failed. Please try again."
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        errorMessage = msg
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = "Network error: could not reach server."
                                }
                            } catch (t: Throwable) {
                                // Catch anything the Exception handler missed (e.g. OOM, linkage)
                                withContext(Dispatchers.Main) {
                                    isLoading = false
                                    errorMessage = "An unexpected error occurred."
                                }
                            }
                        }
                    } catch (_: Throwable) {
                        // Outer safety net — if scope.launch itself throws
                        isLoading = false
                        errorMessage = "Unable to start authentication."
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ElectricCyan,
                    disabledContainerColor = ElectricCyan.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = PrimaryObsidian,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "Continue",
                        color = PrimaryObsidian,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // --- Mode toggle ---
            TextButton(
                onClick = {
                    isRegisterMode = !isRegisterMode
                    errorMessage = null
                }
            ) {
                Text(
                    text = if (isRegisterMode)
                        "Already have an account? Sign in"
                    else
                        "Don't have an account? Create one",
                    color = TextMuted,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * A clean, borderless text field with only a bottom divider line —
 * matching the ultra-minimal aesthetic.
 */
@Composable
private fun MinimalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isPassword: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(
                color = TextLight,
                fontSize = 16.sp
            ),
            cursorBrush = SolidColor(ElectricCyan),
            visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            color = TextMuted,
                            fontSize = 16.sp
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        // Bottom border line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    if (value.isNotEmpty()) ElectricCyan.copy(alpha = 0.6f)
                    else BorderGray
                )
        )
    }
}
