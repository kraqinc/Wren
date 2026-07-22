package com.wren.ide.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.LoginResponse
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.BorderGray
import com.wren.ide.core.theme.EditorYellow
import com.wren.ide.core.theme.ElectricCyan
import com.wren.ide.core.theme.ErrorRed
import com.wren.ide.core.theme.PrimaryObsidian
import com.wren.ide.core.theme.SecondaryCard
import com.wren.ide.core.theme.TerminalGreen
import com.wren.ide.core.theme.TextLight
import com.wren.ide.core.theme.TextMuted
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

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
    var passwordVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun submit() {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isBlank() || password.isBlank()) {
            errorMessage = "Completa tu correo y tu contraseña."
            return
        }
        if (!EMAIL_REGEX.matches(trimmedEmail)) {
            errorMessage = "Escribe un correo válido."
            return
        }
        if (isRegisterMode && password.length < 8) {
            errorMessage = "La contraseña debe tener al menos 8 caracteres."
            return
        }

        isLoading = true
        errorMessage = null

        scope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.post(
                    "/auth",
                    mapOf(
                        "mode" to if (isRegisterMode) "register" else "login",
                        "email" to trimmedEmail,
                        "password" to password
                    )
                )
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val loginData = Gson().fromJson(responseBody, LoginResponse::class.java)
                    sessionManager.jwtToken = loginData.token
                    sessionManager.userEmail = loginData.user.email
                    sessionManager.userRole = loginData.user.role
                    sessionManager.userTier = loginData.user.tier
                    sessionManager.userCredits = loginData.user.balance
                    NetworkClient.setAuthToken(loginData.token)

                    withContext(Dispatchers.Main) {
                        isLoading = false
                        onAuthSuccess()
                    }
                } else {
                    val msg = runCatching {
                        Gson().fromJson(responseBody, Map::class.java)?.get("error") as? String
                    }.getOrNull() ?: "No se pudo autenticar. Inténtalo de nuevo."
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        errorMessage = msg
                    }
                }
            } catch (_: Throwable) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    errorMessage = "No se pudo conectar con el servidor."
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PrimaryObsidian)
    ) {
        PremiumAuthBackground()

        val scroll = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(66.dp)
                    .background(SecondaryCard, RoundedCornerShape(18.dp))
                    .border(1.dp, BorderGray, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Code,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "WREN",
                color = TextLight,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 6.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "AI · Workspace · Shell",
                color = TextMuted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            Surface(
                color = Color(0xFF111215).copy(alpha = 0.92f),
                shape = RoundedCornerShape(28.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray.copy(alpha = 0.85f)),
                shadowElevation = 20.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    ModeSwitch(
                        isRegisterMode = isRegisterMode,
                        onChange = { isRegisterMode = it; errorMessage = null }
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    PremiumInput(
                        value = email,
                        onValueChange = { email = it; errorMessage = null },
                        label = "Correo corporativo",
                        placeholder = "tu@empresa.com",
                        leadingIcon = Icons.Filled.Mail,
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next,
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    PremiumInput(
                        value = password,
                        onValueChange = { password = it; errorMessage = null },
                        label = "Contraseña",
                        placeholder = if (isRegisterMode) "Mínimo 8 caracteres" else "••••••••",
                        leadingIcon = Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        singleLine = true,
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = null,
                                    tint = TextMuted
                                )
                            }
                        },
                        onImeAction = { submit() }
                    )

                    AnimatedVisibility(
                        visible = errorMessage != null,
                        enter = fadeIn(animationSpec = tween(180)) + expandVertically(),
                        exit = fadeOut(animationSpec = tween(150)) + shrinkVertically()
                    ) {
                        Surface(
                            modifier = Modifier.padding(top = 14.dp),
                            color = ErrorRed.copy(alpha = 0.09f),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = ErrorRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = ErrorRed,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = { submit() },
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan,
                            disabledContainerColor = ElectricCyan.copy(alpha = 0.4f),
                            contentColor = PrimaryObsidian
                        )
                    ) {
                        Text(
                            text = if (isLoading) "Verificando..." else if (isRegisterMode) "Crear cuenta" else "Iniciar sesión",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Divider(modifier = Modifier.weight(1f), color = BorderGray)
                        Text(
                            text = "SSO",
                            color = TextMuted,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Divider(modifier = Modifier.weight(1f), color = BorderGray)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlineActionButton(
                            label = "Google",
                            modifier = Modifier.weight(1f),
                            onClick = { errorMessage = "Google Sign-In aún no está conectado." }
                        )
                        OutlineActionButton(
                            label = "GitHub",
                            modifier = Modifier.weight(1f),
                            onClick = { errorMessage = "GitHub Sign-In aún no está conectado." }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Shield,
                    contentDescription = null,
                    tint = TerminalGreen,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "The agent for coding",
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = if (isRegisterMode) "¿Ya tienes cuenta?" else "¿No tienes cuenta?",
                color = TextMuted,
                fontSize = 12.sp
            )

            TextButton(onClick = { isRegisterMode = !isRegisterMode; errorMessage = null }) {
                Text(
                    text = if (isRegisterMode) "Iniciar sesión" else "Crear cuenta",
                    color = TextLight,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PremiumAuthBackground() {
    val transition = rememberInfiniteTransition(label = "auth_bg")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(4500, easing = FastOutSlowInEasing)),
        label = "glow"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF07080A),
                    Color(0xFF0B0D10),
                    Color(0xFF07080A)
                )
            )
        )

        drawCircle(
            color = ElectricCyan.copy(alpha = 0.10f * glow),
            radius = size.minDimension * 0.34f,
            center = Offset(size.width * 0.78f, size.height * 0.18f)
        )

        drawCircle(
            color = TerminalGreen.copy(alpha = 0.10f * glow),
            radius = size.minDimension * 0.26f,
            center = Offset(size.width * 0.22f, size.height * 0.82f)
        )

        val step = 36f
        var x = 0f
        while (x <= size.width) {
            var y = 0f
            while (y <= size.height) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.018f),
                    radius = 1.2f,
                    center = Offset(x, y)
                )
                y += step
            }
            x += step
        }
    }
}

@Composable
private fun ModeSwitch(
    isRegisterMode: Boolean,
    onChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SecondaryCard, RoundedCornerShape(18.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(4.dp)
    ) {
        val active = Modifier.background(Color(0xFF0D0F11), RoundedCornerShape(14.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (!isRegisterMode) active else Modifier)
                .clickable { onChange(false) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Iniciar sesión",
                color = if (!isRegisterMode) TextLight else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .then(if (isRegisterMode) active else Modifier)
                .clickable { onChange(true) }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Crear cuenta",
                color = if (isRegisterMode) TextLight else TextMuted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PremiumInput(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    singleLine: Boolean,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    onImeAction: (() -> Unit)? = null
) {
    Column {
        Text(
            text = label,
            color = TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            placeholder = {
                Text(text = placeholder, color = TextMuted.copy(alpha = 0.7f), fontSize = 14.sp)
            },
            leadingIcon = {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = ElectricCyan
                )
            },
            trailingIcon = trailingIcon,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(
                keyboardType = keyboardType,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(
                onDone = { onImeAction?.invoke() }
            ),
            textStyle = TextStyle(
                color = TextLight,
                fontSize = 15.sp,
                fontFamily = FontFamily.SansSerif
            ),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricCyan,
                unfocusedBorderColor = BorderGray,
                focusedTextColor = TextLight,
                unfocusedTextColor = TextLight,
                cursorColor = ElectricCyan,
                focusedLeadingIconColor = ElectricCyan,
                unfocusedLeadingIconColor = TextMuted,
                focusedTrailingIconColor = TextMuted,
                unfocusedTrailingIconColor = TextMuted
            )
        )
    }
}

@Composable
private fun OutlineActionButton(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(Color.Transparent, RoundedCornerShape(16.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = TextLight,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
