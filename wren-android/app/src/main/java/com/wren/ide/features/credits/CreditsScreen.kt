package com.wren.ide.features.credits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // States
    var historyLogs by remember { mutableStateOf<List<CreditLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    // Fetch credits history statement
    LaunchedEffect(Unit) {
        isLoading = true
        scope.launch(Dispatchers.IO) {
            try {
                val response = NetworkClient.get("/credits/history")
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val data = Gson().fromJson(body, CreditHistoryResponse::class.java)
                    withContext(Dispatchers.Main) {
                        historyLogs = data.logs
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Billetera Wren", color = TextLight) },
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
            // Balance Panel Card
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MonetizationOn,
                            contentDescription = null,
                            tint = EditorYellow,
                            modifier = Modifier.size(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("BALANCE DE PUNTOS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${sessionManager.userCredits} Pts",
                            color = EditorYellow,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nivel de Cuenta: Plan ${sessionManager.userTier}",
                                color = ElectricCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            actionMessage?.let { msg ->
                item {
                    Text(
                        text = msg,
                        color = TerminalGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            // Pricing Package Header
            item {
                Text("RECARGAR PUNTOS & SUSCRIPCIONES", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Package Options List
            val packages = listOf(
                Triple("basic_100", "Recarga Inicial (+100 Puntos)", "$1.99 USD"),
                Triple("premium_500", "Recarga Premium (+500 Puntos)", "$6.99 USD"),
                Triple("subscription_monthly", "Plan Suscriptor Mensual (+1000 Pts/Mes)", "$9.99 USD / Mes")
            )

            items(packages) { (pkgId, title, price) ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                        .clickable {
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val body = mapOf("packageId" to pkgId)
                                    val response = NetworkClient.post("/credits/recharge", body)
                                    if (response.isSuccessful) {
                                        val b = response.body?.string()
                                        val map = Gson().fromJson(b, Map::class.java)
                                        val newBal = (map["newBalance"] as Double).toInt()
                                        val newTier = map["tier"] as String
                                        
                                        // Update Session storage cache
                                        sessionManager.userCredits = newBal
                                        sessionManager.userTier = newTier

                                        // Refresh transaction logs history
                                        val hRes = NetworkClient.get("/credits/history")
                                        val hLogs = if (hRes.isSuccessful) {
                                            Gson().fromJson(hRes.body?.string(), CreditHistoryResponse::class.java).logs
                                        } else emptyList()

                                        withContext(Dispatchers.Main) {
                                            historyLogs = hLogs
                                            actionMessage = "Compra procesada con éxito: $title"
                                            isLoading = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            actionMessage = "Error en el servidor al procesar el pago."
                                            isLoading = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        actionMessage = "Error de red al procesar transacción."
                                        isLoading = false
                                    }
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(if (pkgId.startsWith("sub")) "Incluye acceso prioritario" else "Puntos de consumo", color = TextMuted, fontSize = 11.sp)
                        }
                        Text(price, color = ElectricCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
                    }
                }
            }

            // Statements History Header
            item {
                Text("HISTORIAL DE TRANSACCIONES", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            if (isLoading && historyLogs.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(24.dp))
                    }
                }
            } else if (historyLogs.isEmpty()) {
                item {
                    Text("No se registran movimientos en tu cuenta.", color = TextMuted, fontSize = 13.sp)
                }
            } else {
                items(historyLogs) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = log.reason, color = TextLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text(text = log.timestamp.take(16).replace("T", " "), color = TextMuted, fontSize = 10.sp)
                            }
                            Text(
                                text = if (log.amount > 0) "+${log.amount} Pts" else "${log.amount} Pts",
                                color = if (log.amount > 0) TerminalGreen else ErrorRed,
                                fontWeight = FontWeight.Black,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
