package com.wren.ide.features.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.wren.ide.core.network.*
import com.wren.ide.core.storage.SessionManager
import com.wren.ide.core.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAgentScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    
    // AI Chat parameters
    var promptInput by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("chat") } // chat, planificador, editor, terminal
    var aiLogs by remember { mutableStateOf<List<Pair<String, String>>>(listOf("Asistente" to "¡Hola! ¿En qué modo deseas trabajar hoy? Puedes seleccionar Chat estándar, Planificador autónomo, Editor o Terminal en la barra superior.")) }
    
    // Safety Actions Queue
    var proposedActions by remember { mutableStateOf<List<ChatAction>>(emptyList()) }
    var actionExecutionLog by remember { mutableStateOf<String?>(null) }
    
    // Flags
    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Asistente AI Agent", color = TextLight) },
                navigationIcon = {
                    IconButton(onClick = onNavBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = ElectricCyan)
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = "Points", tint = EditorYellow)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${sessionManager.userCredits} Puntos",
                            color = EditorYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryCard)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PrimaryObsidian)
        ) {
            // Mode Selectors Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryCard)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                val modes = listOf(
                    Triple("Standard", "chat", Icons.Filled.Chat),
                    Triple("Planner", "planificador", Icons.Filled.Assignment),
                    Triple("Editor", "editor", Icons.Filled.Edit),
                    Triple("Terminal", "terminal", Icons.Filled.Terminal)
                )

                modes.forEach { (label, modeKey, icon) ->
                    val isActive = selectedMode == modeKey
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clickable { selectedMode = modeKey }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isActive) ElectricCyan else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = label,
                            color = if (isActive) ElectricCyan else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Main chat logs
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(aiLogs) { (sender, msg) ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (sender == "Usuario") BorderGray else SecondaryCard,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Text(
                            text = sender,
                            color = if (sender == "Usuario") ElectricCyan else EditorYellow,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = msg,
                            color = TextLight,
                            fontSize = 14.sp
                        )
                    }
                }

                // If proposed actions exist, render them inside the safe confirmation dialog area
                if (proposedActions.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SecondaryCard),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, EditorYellow, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Warning, contentDescription = null, tint = EditorYellow)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("ACCIONES PROPUESTAS - REVISIÓN OBLIGATORIA", color = EditorYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                                Text("El planificador de Wren AI ha sugerido aplicar los siguientes cambios estructurales. Revísalos antes de confirmar:", color = TextMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 8.dp))

                                proposedActions.forEach { action ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Column {
                                            Text(text = "${action.type} -> ${action.path ?: action.command ?: ""}", color = TextLight, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                            Text(text = action.description, color = TextMuted, fontSize = 11.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Button(
                                    onClick = {
                                        isLoading = true
                                        scope.launch(Dispatchers.IO) {
                                            try {
                                                // Securely execute action charging point
                                                val body = mapOf(
                                                    "action" to proposedActions.first().type,
                                                    "details" to proposedActions
                                                )
                                                val response = NetworkClient.post("/ai/agent/execute", body)
                                                if (response.isSuccessful) {
                                                    val resBody = response.body?.string()
                                                    val data = Gson().fromJson(resBody, ChatResponse::class.java)
                                                    
                                                    // Deduct points
                                                    sessionManager.userCredits = data.remainingCredits

                                                    withContext(Dispatchers.Main) {
                                                        actionExecutionLog = "Acción ejecutada con éxito. Costo: 1 Punto."
                                                        proposedActions = emptyList() // clear queue
                                                        isLoading = false
                                                    }
                                                } else {
                                                    withContext(Dispatchers.Main) {
                                                        actionExecutionLog = "Error al ejecutar la acción en el servidor."
                                                        isLoading = false
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                withContext(Dispatchers.Main) {
                                                    actionExecutionLog = "Error de red al aplicar cambios."
                                                    isLoading = false
                                                }
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = EditorYellow),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Confirmar y Ejecutar Acción (-1 Punto)", color = PrimaryObsidian, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { proposedActions = emptyList() },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                ) {
                                    Text("Rechazar y Cancelar", color = ErrorRed)
                                }
                            }
                        }
                    }
                }

                actionExecutionLog?.let {
                    item {
                        Text(
                            text = it,
                            color = TerminalGreen,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            errorMsg?.let {
                Text(
                    text = it,
                    color = ErrorRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Bottom Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SecondaryCard)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BasicTextField(
                    value = promptInput,
                    onValueChange = { promptInput = it },
                    textStyle = TextStyle(color = TextLight, fontSize = 14.sp),
                    cursorBrush = SolidColor(ElectricCyan),
                    modifier = Modifier
                        .weight(1f)
                        .background(PrimaryObsidian, RoundedCornerShape(8.dp))
                        .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                if (isLoading) {
                    CircularProgressIndicator(color = ElectricCyan, modifier = Modifier.size(28.dp))
                } else {
                    IconButton(
                        onClick = {
                            if (promptInput.isBlank()) return@IconButton
                            if (sessionManager.userCredits <= 0) {
                                errorMsg = "Puntos insuficientes. Por favor, recarga tu balance."
                                return@IconButton
                            }
                            
                            val userPrompt = promptInput.trim()
                            aiLogs = aiLogs + ("Usuario" to userPrompt)
                            promptInput = ""
                            isLoading = true
                            errorMsg = null
                            actionExecutionLog = null

                            scope.launch(Dispatchers.IO) {
                                try {
                                    val body = mapOf(
                                        "prompt" to userPrompt,
                                        "mode" to selectedMode
                                    )
                                    val response = NetworkClient.post("/ai/chat", body)
                                    if (response.isSuccessful) {
                                        val resBodyStr = response.body?.string()
                                        val data = Gson().fromJson(resBodyStr, ChatResponse::class.java)

                                        // Update credit balance
                                        sessionManager.userCredits = data.remainingCredits

                                        withContext(Dispatchers.Main) {
                                            aiLogs = aiLogs + ("Asistente" to data.response)
                                            data.actions?.let {
                                                if (it.isNotEmpty()) {
                                                    proposedActions = it
                                                }
                                            }
                                            isLoading = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            errorMsg = "Error en el servidor al enviar el prompt."
                                            isLoading = false
                                        }
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMsg = "Error de red al conectar al agente."
                                        isLoading = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .background(ElectricCyan, RoundedCornerShape(8.dp))
                            .size(40.dp)
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = PrimaryObsidian, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
