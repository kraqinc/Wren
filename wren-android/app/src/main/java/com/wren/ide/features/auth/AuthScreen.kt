package com.wren.ide.features.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.LoginResponse
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
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
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryObsidian),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SecondaryCard)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "WREN IDE",
                    color = ElectricCyan,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Cursor-in-the-Cloud Studio",
                    color = TextMuted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo Electrónico", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = ElectricCyan
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = TextMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderGray,
                        focusedLabelColor = ElectricCyan
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )

                errorMessage?.let {
                    Text(
                        text = it,
                        color = ErrorRed,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        color = ElectricCyan,
                        modifier = Modifier.size(36.dp).padding(bottom = 16.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            if (email.isBlank() || password.isBlank()) {
                                errorMessage = "Por favor, llena todos los campos."
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
                                        val loginData = Gson().fromJson(responseBodyStr, LoginResponse::class.java)
                                        
                                        // Save Session Details
                                        sessionManager.jwtToken = loginData.token
                                        sessionManager.userEmail = loginData.user.email
                                        sessionManager.userRole = loginData.user.role
                                        sessionManager.userTier = loginData.user.tier
                                        sessionManager.userCredits = loginData.user.balance
                                        
                                        // Update static network helper header
                                        NetworkClient.setAuthToken(loginData.token)

                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            onAuthSuccess()
                                        }
                                    } else {
                                        val errMap = try { Gson().fromJson(responseBodyStr, Map::class.java) } catch (e: Exception) { null }
                                        val msg = errMap?.get("error") as? String ?: "Fallo de autenticación."
                                        withContext(Dispatchers.Main) {
                                            isLoading = false
                                            errorMessage = msg
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        errorMessage = "Error de red: no se pudo contactar al servidor."
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Crear Cuenta" else "Iniciar Sesión",
                            color = PrimaryObsidian,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    },
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia Sesión" else "¿No tienes cuenta? Regístrate gratis",
                        color = ElectricCyan,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}
