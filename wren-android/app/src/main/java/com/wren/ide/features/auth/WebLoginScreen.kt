package com.wren.ide.features.auth

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.gson.Gson
import com.wren.ide.core.network.LoginResponse
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.network.User
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.ElectricCyan
import com.wren.ide.core.theme.ErrorRed
import com.wren.ide.core.theme.PrimaryObsidian
import com.wren.ide.core.theme.SecondaryCard
import com.wren.ide.core.theme.TextLight
import com.wren.ide.core.theme.TextMuted

// URL de la página de login. Apunta aquí una vez que wren-accountsis.onrender.com
// esté vivo y login-wren.vercel.app hable con ella.
private const val LOGIN_URL = "https://login-wren.vercel.app"

/**
 * Contrato entre este WebView y la página en [LOGIN_URL]:
 *
 * Al iniciar sesión con éxito, el JS de la página debe llamar:
 *   window.WrenAuthBridge.onLoginSuccess(JSON.stringify({
 *     token: "<jwt>",
 *     user: { id, email, role, tier, balance }
 *   }))
 *
 * Si quiere mostrar un error nativo:
 *   window.WrenAuthBridge.onLoginError("mensaje")
 *
 * Fallback: si la página en vez de eso redirige a una URL con `?token=...`,
 * este WebView también lo detecta y completa el login solo con el token
 * (sin datos de usuario) — prefiere el bridge JS si controlas la página.
 */
private class WrenAuthBridge(
    private val onSuccess: (token: String, user: User?) -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onLoginSuccess(jsonPayload: String) {
        try {
            val parsed = Gson().fromJson(jsonPayload, LoginResponse::class.java)
            onSuccess(parsed.token, parsed.user)
        } catch (_: Exception) {
            onError("La página de inicio de sesión devolvió una respuesta inválida.")
        }
    }

    @JavascriptInterface
    fun onLoginError(message: String) {
        onError(message)
    }
}

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebLoginScreen(
    sessionManager: SessionManager,
    onAuthSuccess: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var reloadTrigger by remember { mutableStateOf(0) }

    fun completeLogin(token: String, user: User?) {
        sessionManager.jwtToken = token
        user?.let {
            sessionManager.userEmail = it.email
            sessionManager.userRole = it.role
            sessionManager.userTier = it.tier
            sessionManager.userCredits = it.balance
        }
        NetworkClient.setAuthToken(token)
        onAuthSuccess()
    }

    Column(modifier = Modifier.fillMaxSize().background(PrimaryObsidian)) {
        TopAppBar(
            title = { Text("Iniciar sesión", color = TextLight, fontSize = 16.sp) },
            navigationIcon = {
                IconButton(onClick = { webViewRef?.let { if (it.canGoBack()) it.goBack() } }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Atrás", tint = TextLight)
                }
            },
            actions = {
                IconButton(onClick = { reloadTrigger++; loadError = null; isLoading = true }) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Recargar", tint = TextMuted)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryCard)
        )

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (loadError == null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            addJavascriptInterface(
                                WrenAuthBridge(
                                    onSuccess = { token, user -> completeLogin(token, user) },
                                    onError = { msg -> loadError = msg }
                                ),
                                "WrenAuthBridge"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun onReceivedError(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                    error: WebResourceError?
                                ) {
                                    if (request?.isForMainFrame == true) {
                                        isLoading = false
                                        loadError = "No se pudo cargar la página de inicio de sesión. Comprueba tu conexión."
                                    }
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString() ?: return false
                                    val token = extractQueryParam(url, "token")
                                    if (token != null) {
                                        completeLogin(token, null)
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(LOGIN_URL)
                            webViewRef = this
                        }
                    },
                    update = { view ->
                        if (reloadTrigger > 0) view.reload()
                    }
                )
            }

            if (isLoading && loadError == null) {
                Box(modifier = Modifier.fillMaxSize().background(PrimaryObsidian), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Cargando…",
                        color = ElectricCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            loadError?.let { message ->
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(message, color = ErrorRed, fontSize = 14.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { loadError = null; isLoading = true; reloadTrigger++ },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                    ) {
                        Text("Reintentar", color = PrimaryObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/** Extractor mínimo de query params, para no meter una dependencia extra solo por esto. */
private fun extractQueryParam(url: String, key: String): String? {
    return try {
        android.net.Uri.parse(url).getQueryParameter(key)
    } catch (_: Exception) {
        null
    }
}
