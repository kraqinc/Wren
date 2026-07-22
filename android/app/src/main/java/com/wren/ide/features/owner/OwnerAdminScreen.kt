package com.wren.ide.features.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.*
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Same glass-panel language used across every other screen in the app. */
@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = ElectricCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(SecondaryCard.copy(alpha = 0.55f), SecondaryCard.copy(alpha = 0.30f))))
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(16.dp)),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAdminScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var serverMetrics by remember { mutableStateOf<ServerMetrics?>(null) }
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    var pendingRecharges by remember { mutableStateOf<List<PendingRecharge>>(emptyList()) }

    var selectedUserForCredits by remember { mutableStateOf<User?>(null) }
    var overrideAmountInput by remember { mutableStateOf("") }
    var overrideReasonInput by remember { mutableStateOf("") }
    var actionResultMsg by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var processingRequestId by remember { mutableStateOf<String?>(null) }

    suspend fun refreshAll() {
        try {
            val statsRes = NetworkClient.get("/owner?view=stats")
            val statsData = if (statsRes.isSuccessful) {
                Gson().fromJson(statsRes.body?.string(), ServerStatsResponse::class.java).metrics
            } else null

            val usersRes = NetworkClient.get("/owner?view=users")
            val usersData = if (usersRes.isSuccessful) {
                Gson().fromJson(usersRes.body?.string(), UsersListResponse::class.java).users
            } else emptyList()

            val auditRes = NetworkClient.get("/owner?view=audit-logs")
            val auditData = if (auditRes.isSuccessful) {
                Gson().fromJson(auditRes.body?.string(), AuditLogsResponse::class.java).logs
            } else emptyList()

            val rechargesRes = NetworkClient.get("/owner?view=pending-recharges")
            val rechargesData = if (rechargesRes.isSuccessful) {
                Gson().fromJson(rechargesRes.body?.string(), PendingRechargesResponse::class.java).requests
            } else emptyList()

            withContext(Dispatchers.Main) {
                serverMetrics = statsData
                userList = usersData
                auditLogs = auditData
                pendingRecharges = rechargesData
                isLoading = false
            }
        } catch (_: Exception) {
            withContext(Dispatchers.Main) { isLoading = false }
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch(Dispatchers.IO) { refreshAll() }
    }

    fun resolveRecharge(requestId: String, approve: Boolean) {
        processingRequestId = requestId
        errorMsg = null
        scope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.post(
                    "/owner",
                    mapOf(
                        "action" to if (approve) "approve" else "reject",
                        "requestId" to requestId
                    )
                )
                if (response.isSuccessful) {
                    refreshAll()
                    withContext(Dispatchers.Main) {
                        actionResultMsg = if (approve) "Recarga aprobada y acreditada." else "Recarga rechazada."
                        processingRequestId = null
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        errorMsg = "No se pudo procesar la solicitud."
                        processingRequestId = null
                    }
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    errorMsg = "Error de red al procesar la recarga."
                    processingRequestId = null
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(PrimaryObsidian, Color(0xFF0B1020), PrimaryObsidian)))
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Shield, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Consola del Propietario", color = TextLight, fontSize = 15.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = TextLight)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item { SectionLabel("ESTADÍSTICAS DEL SERVIDOR") }

                serverMetrics?.let { metrics ->
                    item {
                        // Bento 2x2 grid of stat tiles
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile("Usuarios", "${metrics.totalUsers}", ElectricCyan, Modifier.weight(1f))
                                StatTile("Puntos Totales", "${metrics.circulatingCredits}", EditorYellow, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StatTile("Proyectos", "${metrics.totalProjects}", TextLight, Modifier.weight(1f))
                                StatTile("Puntos Gastados", "${metrics.historicalSpentCredits}", ErrorRed, Modifier.weight(1f))
                            }
                        }
                    }
                }

                errorMsg?.let { item { Text(it, color = ErrorRed, fontSize = 12.5.sp, fontWeight = FontWeight.Medium) } }
                actionResultMsg?.let { item { Text(it, color = TerminalGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold) } }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SectionLabel("RECARGAS PENDIENTES")
                        if (pendingRecharges.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(EditorYellow.copy(alpha = 0.18f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${pendingRecharges.size}", color = EditorYellow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (pendingRecharges.isEmpty()) {
                    item { Text("No hay recargas esperando verificación.", color = TextMuted, fontSize = 13.sp) }
                } else {
                    items(pendingRecharges, key = { it.id }) { req ->
                        GlassPanel(modifier = Modifier.fillMaxWidth(), accent = EditorYellow) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(req.user_email, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Text(req.package_id, color = TextMuted, fontSize = 11.sp)
                                    }
                                    Text("+${req.credit_amount} Pts", color = EditorYellow, fontWeight = FontWeight.Black, fontSize = 14.sp)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Código:", color = TextMuted, fontSize = 11.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        req.reference_code,
                                        color = ElectricCyan,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(req.price_label, color = TextMuted, fontSize = 11.sp)
                                Spacer(modifier = Modifier.height(12.dp))

                                if (processingRequestId == req.id) {
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        Text("…", color = ElectricCyan, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                        TextButton(onClick = { resolveRecharge(req.id, approve = false) }) {
                                            Text("Rechazar", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { resolveRecharge(req.id, approve = true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.height(34.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryObsidian, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Confirmar pago", color = PrimaryObsidian, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { SectionLabel("AJUSTE MANUAL DE CRÉDITOS") }

                item {
                    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = TerminalGreen) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                selectedUserForCredits?.let { "Usuario seleccionado: ${it.email}" } ?: "Toca un usuario abajo para seleccionarlo",
                                color = if (selectedUserForCredits != null) ElectricCyan else TextMuted,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = overrideAmountInput,
                                onValueChange = { overrideAmountInput = it },
                                label = { Text("Cantidad (+/-)", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight, unfocusedTextColor = TextLight,
                                    focusedBorderColor = ElectricCyan, unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                            )
                            OutlinedTextField(
                                value = overrideReasonInput,
                                onValueChange = { overrideReasonInput = it },
                                label = { Text("Motivo", color = TextMuted) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextLight, unfocusedTextColor = TextLight,
                                    focusedBorderColor = ElectricCyan, unfocusedBorderColor = BorderGray
                                ),
                                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                            )
                            Button(
                                onClick = {
                                    val target = selectedUserForCredits
                                    val amt = overrideAmountInput.toIntOrNull()
                                    val rsn = overrideReasonInput.trim()
                                    if (target == null || amt == null || rsn.isBlank()) {
                                        actionResultMsg = null
                                        errorMsg = "Selecciona un usuario y completa cantidad y motivo."
                                        return@Button
                                    }
                                    errorMsg = null
                                    isLoading = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val body = mapOf(
                                                "action" to "adjust-credits",
                                                "userId" to target.id,
                                                "amount" to amt,
                                                "reason" to rsn
                                            )
                                            val response = NetworkClient.post("/owner", body)
                                            if (response.isSuccessful) {
                                                refreshAll()
                                                withContext(Dispatchers.Main) {
                                                    actionResultMsg = "Créditos modificados. Log de auditoría creado."
                                                    overrideAmountInput = ""
                                                    overrideReasonInput = ""
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    errorMsg = "Error del servidor al modificar créditos."
                                                    isLoading = false
                                                }
                                            }
                                        } catch (_: Exception) {
                                            withContext(Dispatchers.Main) {
                                                errorMsg = "Error de red al aplicar ajuste."
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Aplicar Ajuste", color = PrimaryObsidian, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                item { SectionLabel("USUARIOS (${userList.size})") }

                items(userList, key = { it.id }) { user ->
                    val isSelected = selectedUserForCredits?.id == user.id
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth().clickable { selectedUserForCredits = user },
                        accent = if (isSelected) ElectricCyan else BorderGray
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.email, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("ID: ${user.id.take(8)}… · Rol: ${user.role}", color = TextMuted, fontSize = 10.sp)
                            }
                            Text("${user.balance} Pts", color = EditorYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                        }
                    }
                }

                item { SectionLabel("LOGS DE AUDITORÍA (RBAC)") }

                items(auditLogs, key = { it.id }) { log ->
                    GlassPanel(
                        modifier = Modifier.fillMaxWidth(),
                        accent = if (log.action.contains("UNAUTHORIZED")) ErrorRed else TerminalGreen
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    log.action,
                                    color = if (log.action.contains("UNAUTHORIZED")) ErrorRed else TerminalGreen,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                                Text(log.timestamp.take(16).replace("T", " "), color = TextMuted, fontSize = 9.sp)
                            }
                            Text("Actor: ${log.actor_email}", color = TextLight, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 2.dp))
                            log.details?.let {
                                Text(it, color = TextMuted, fontSize = 11.sp, fontFamily = FontFamily.Monospace, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = TextLight, fontSize = 14.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
}

@Composable
private fun StatTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    GlassPanel(modifier = modifier, accent = accent) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(label, color = TextMuted, fontSize = 11.sp)
            Text(value, color = accent, fontSize = 20.sp, fontWeight = FontWeight.Black)
        }
    }
}
