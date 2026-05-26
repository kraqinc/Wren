package com.wren.ide.features.owner

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wren.ide.core.network.*
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OwnerAdminScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    // Dashboard Data
    var serverMetrics by remember { mutableStateOf<ServerMetrics?>(null) }
    var userList by remember { mutableStateOf<List<User>>(emptyList()) }
    var auditLogs by remember { mutableStateOf<List<AuditLog>>(emptyList()) }
    
    // UI selections / modifiers
    var selectedUserForCredits by remember { mutableStateOf<User?>(null) }
    var overrideAmountInput by remember { mutableStateOf("") }
    var overrideReasonInput by remember { mutableStateOf("") }
    var actionResultMsg by remember { mutableStateOf<String?>(null) }
    
    // Loading
    var isLoading by remember { mutableStateOf(false) }

    // Init data load
    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                // 1. Fetch metrics
                val statsRes = NetworkClient.get("/owner/stats")
                val statsData = if (statsRes.isSuccessful) {
                    Gson().fromJson(statsRes.body?.string(), ServerStatsResponse::class.java).metrics
                } else null

                // 2. Fetch users list
                val usersRes = NetworkClient.get("/owner/users")
                val usersData: List<User> = if (usersRes.isSuccessful) {
                    val mapType = object : TypeToken<Map<String, List<User>>>() {}.type
                    val map: Map<String, List<User>> = Gson().fromJson(usersRes.body?.string(), mapType)
                    map["users"] ?: emptyList()
                } else emptyList()

                // 3. Fetch audit security logs
                val auditRes = NetworkClient.get("/owner/audit-logs")
                val auditData = if (auditRes.isSuccessful) {
                    Gson().fromJson(auditRes.body?.string(), AuditLogsResponse::class.java).logs
                } else emptyList()

                withContext(Dispatchers.Main) {
                    serverMetrics = statsData
                    userList = usersData
                    auditLogs = auditData
                    isLoading = false
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Shield, contentDescription = null, tint = TerminalGreen)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Consola del Propietario", color = TextLight)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = ElectricCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryCard)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PrimaryObsidian)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Metrics Summary
            item {
                Text("ESTADÍSTICAS DEL SERVIDOR", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            serverMetrics?.let { metrics ->
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                                modifier = Modifier.weight(1f).border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Usuarios", color = TextMuted, fontSize = 11.sp)
                                    Text("${metrics.totalUsers}", color = ElectricCyan, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                                modifier = Modifier.weight(1f).border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Puntos Totales", color = TextMuted, fontSize = 11.sp)
                                    Text("${metrics.circulatingCredits} Pts", color = EditorYellow, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                                modifier = Modifier.weight(1f).border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Proyectos Activos", color = TextMuted, fontSize = 11.sp)
                                    Text("${metrics.totalProjects}", color = TextLight, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                                modifier = Modifier.weight(1f).border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Puntos Consumidos", color = TextMuted, fontSize = 11.sp)
                                    Text("${metrics.historicalSpentCredits}", color = ErrorRed, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }
            } ?: item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(24.dp))
                }
            }

            // Credits adjustment Overrides Card
            item {
                Text("AJUSTE DE CRÉDITOS AUDITADO", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = selectedUserForCredits?.let { "Ajustando: ${it.email}" } ?: "Selecciona un usuario en la lista de abajo",
                            color = if (selectedUserForCredits != null) ElectricCyan else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = overrideAmountInput,
                            onValueChange = { overrideAmountInput = it },
                            label = { Text("Monto (positivo/negativo)", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )

                        OutlinedTextField(
                            value = overrideReasonInput,
                            onValueChange = { overrideReasonInput = it },
                            label = { Text("Razón del Ajuste", color = TextMuted) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextLight,
                                unfocusedTextColor = TextLight,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = BorderGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        )

                        actionResultMsg?.let {
                            Text(
                                text = it,
                                color = TerminalGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val target = selectedUserForCredits
                                val amt = overrideAmountInput.toIntOrNull()
                                val rsn = overrideReasonInput.trim()

                                if (target == null || amt == null || rsn.isBlank()) {
                                    actionResultMsg = "Por favor, llena los campos y selecciona usuario."
                                    return@Button
                                }

                                isLoading = true
                                scope.launch(Dispatchers.IO) {
                                    try {
                                        val body = mapOf("amount" to amt, "reason" to rsn)
                                        val response = NetworkClient.post("/owner/users/${target.id}/credits", body)
                                        if (response.isSuccessful) {
                                            // Reload Audit Logs and users list
                                            val usersRes = NetworkClient.get("/owner/users")
                                            val mapType = object : TypeToken<Map<String, List<User>>>() {}.type
                                            val usersData: List<User> = Gson().fromJson(usersRes.body?.string(), mapType)["users"] ?: emptyList()
                                            
                                            val auditRes = NetworkClient.get("/owner/audit-logs")
                                            val auditData = Gson().fromJson(auditRes.body?.string(), AuditLogsResponse::class.java).logs

                                            withContext(Dispatchers.Main) {
                                                userList = usersData
                                                auditLogs = auditData
                                                actionResultMsg = "Créditos modificados. Log de auditoría creado."
                                                overrideAmountInput = ""
                                                overrideReasonInput = ""
                                                isLoading = false
                                            }
                                        } else {
                                            withContext(Dispatchers.Main) {
                                                actionResultMsg = "Error del servidor al modificar créditos."
                                                isLoading = false
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            actionResultMsg = "Error de red al aplicar ajuste."
                                            isLoading = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Aplicar Ajuste de Créditos", color = PrimaryObsidian, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // User Selection Header
            item {
                Text("SELECCIÓN DE USUARIO", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // User Items
            items(userList) { user ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (selectedUserForCredits?.id == user.id) ElectricCyan else Color.Transparent,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { selectedUserForCredits = user }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.email, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("ID: ${user.id.take(8)}... | Rol: ${user.role}", color = TextMuted, fontSize = 10.sp)
                        }
                        Text("${user.balance} Pts", color = EditorYellow, fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }

            // Security logs audits Header
            item {
                Text("LOGS DE AUDITORÍA DE SEGURIDAD (RBAC)", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Audit Records List
            items(auditLogs) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.action,
                                color = if (log.action.contains("UNAUTHORIZED")) ErrorRed else TerminalGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = log.timestamp.take(16).replace("T", " "),
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }
                        Text(
                            text = "Actor: ${log.actor_email}",
                            color = TextLight,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                        log.details?.let {
                            Text(
                                text = it,
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
