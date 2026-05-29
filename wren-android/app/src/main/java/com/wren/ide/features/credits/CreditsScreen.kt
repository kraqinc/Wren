package com.wren.ide.features.credits

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.*
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Payment package definition.
 * @param id Backend package identifier
 * @param title Human-readable label
 * @param points Amount of Wren points granted
 * @param priceUsd Dollar amount for PayPal redirect
 * @param priceLabel Formatted display price
 * @param isSubscription Whether this is a recurring package
 */
data class PaymentPackage(
    val id: String,
    val title: String,
    val points: Int,
    val priceUsd: String,
    val priceLabel: String,
    val isSubscription: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditsScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // States
    var historyLogs by remember { mutableStateOf<List<CreditLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<String?>(null) }
    var selectedPackage by remember { mutableStateOf<PaymentPackage?>(null) }
    var showPaymentDialog by remember { mutableStateOf(false) }

    // Payment packages
    val packages = remember {
        listOf(
            PaymentPackage("basic_100", "Starter Pack", 100, "1.99", "$1.99 USD"),
            PaymentPackage("premium_500", "Premium Pack", 500, "6.99", "$6.99 USD"),
            PaymentPackage("pro_1500", "Pro Pack", 1500, "14.99", "$14.99 USD"),
            PaymentPackage("ultra_5000", "Ultra Pack", 5000, "39.99", "$39.99 USD"),
            PaymentPackage(
                "subscription_monthly", "Monthly Subscriber",
                1000, "9.99", "$9.99 USD / Month",
                isSubscription = true
            )
        )
    }

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

            // Payment Methods Header
            item {
                Text("MÉTODOS DE PAGO", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // PayPal / Google Pay Info
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().border(1.dp, BorderGray, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Payment, contentDescription = null, tint = Color(0xFF0070BA), modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("PayPal", color = Color(0xFF0070BA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Text("✓ Activo", color = TerminalGreen, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Pago seguro vía paypal.me/KraqPro",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Pricing Package Header
            item {
                Text("RECARGAR PUNTOS & SUSCRIPCIONES", color = TextLight, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            // Package Options List
            items(packages) { pkg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 1.dp,
                            color = if (selectedPackage?.id == pkg.id) ElectricCyan else BorderGray,
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable {
                            selectedPackage = pkg
                            showPaymentDialog = true
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pkg.title, color = TextLight, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                "+${pkg.points} Puntos" + if (pkg.isSubscription) " / Mes" else "",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(pkg.priceLabel, color = ElectricCyan, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Icon(
                                Icons.Filled.ShoppingCart,
                                contentDescription = "Comprar",
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
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

    // Payment Confirmation Dialog with PayPal redirect
    if (showPaymentDialog && selectedPackage != null) {
        val pkg = selectedPackage!!
        AlertDialog(
            onDismissRequest = {
                showPaymentDialog = false
                selectedPackage = null
            },
            title = {
                Text("Confirmar Compra", color = TextLight, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = pkg.title,
                        color = ElectricCyan,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Divider(color = BorderGray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Puntos:", color = TextMuted)
                        Text("+${pkg.points} Pts", color = EditorYellow, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Precio:", color = TextMuted)
                        Text(pkg.priceLabel, color = ElectricCyan, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Se abrirá PayPal para completar el pago de forma segura.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Open PayPal.me URL with the amount
                        val paypalUrl = "https://paypal.me/KraqPro/${pkg.priceUsd}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paypalUrl))
                        context.startActivity(intent)

                        // After payment redirect, register purchase on backend
                        isLoading = true
                        showPaymentDialog = false
                        scope.launch(Dispatchers.IO) {
                            try {
                                val body = mapOf("packageId" to pkg.id)
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
                                        actionMessage = "✓ Pago de ${pkg.priceLabel} procesado vía PayPal — +${pkg.points} Pts acreditados"
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
                        selectedPackage = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0070BA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Payment, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pagar con PayPal", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPaymentDialog = false
                    selectedPackage = null
                }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = SecondaryCard
        )
    }
}
