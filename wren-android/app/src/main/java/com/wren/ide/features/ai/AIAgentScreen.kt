package com.wren.ide.features.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Glass card: translucent fill + soft light-catching border, the base unit of
 * the "spatial" bento layout. True Gaussian blur-behind isn't available in
 * this Compose version without an extra rendering pipeline, so depth here
 * comes from layered translucency + a bright top-edge highlight instead.
 */
@Composable
private fun GlassPanel(
    modifier: Modifier = Modifier,
    accent: Color = ElectricCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(SecondaryCard.copy(alpha = 0.55f), SecondaryCard.copy(alpha = 0.30f))
                )
            )
            .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
        content = content
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAgentScreen(
    sessionManager: SessionManager,
    onNavBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var promptInput by remember { mutableStateOf("") }
    var selectedMode by remember { mutableStateOf("chat") }

    var aiLogs by remember {
        mutableStateOf<List<Pair<String, String>>>(
            listOf(
                "Asistente" to "¡Hola! Soy Wren AI Agent. Pídeme crear archivos, planear una función o generar comandos de terminal."
            )
        )
    }

    var proposedActions by remember { mutableStateOf<List<ChatAction>>(emptyList()) }
    var actionExecutionLog by remember { mutableStateOf<String?>(null) }
    var expandedDiffIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var acceptedActionIndices by remember { mutableStateOf<Set<Int>>(emptySet()) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(PrimaryObsidian, Color(0xFF0B1020), PrimaryObsidian)
                )
            )
    ) {
        // Soft ambient glow blobs behind the glass panels — this is where the
        // "spatial" depth comes from: fixed, not animated, so it reads as
        // atmosphere rather than a toy.
        Box(
            modifier = Modifier
                .size(260.dp)
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .background(
                    Brush.radialGradient(listOf(ElectricCyan.copy(alpha = 0.12f), Color.Transparent)),
                    shape = RoundedCornerShape(130.dp)
                )
        )
        Box(
            modifier = Modifier
                .size(220.dp)
                .align(Alignment.BottomStart)
                .offset(x = (-50).dp, y = 60.dp)
                .background(
                    Brush.radialGradient(listOf(EditorYellow.copy(alpha = 0.08f), Color.Transparent)),
                    shape = RoundedCornerShape(110.dp)
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Wren AI Agent", color = TextLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavBack) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Regresar", tint = TextLight)
                        }
                    },
                    actions = {
                        Row(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .background(EditorYellow.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                                .border(1.dp, EditorYellow.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.MonetizationOn, contentDescription = null, tint = EditorYellow, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${sessionManager.userCredits}", color = EditorYellow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                Column {
                    errorMsg?.let {
                        Text(
                            text = it,
                            color = ErrorRed,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                    PromptInputBar(
                        value = promptInput,
                        onValueChange = { promptInput = it },
                        isLoading = isLoading,
                        onSend = {
                            if (promptInput.isBlank()) return@PromptInputBar
                            if (sessionManager.userCredits <= 0) {
                                errorMsg = "Puntos insuficientes. Recarga tu balance para seguir usando el agente."
                                return@PromptInputBar
                            }

                            val userPrompt = promptInput.trim()
                            aiLogs = aiLogs + ("Usuario" to userPrompt)
                            promptInput = ""
                            isLoading = true
                            errorMsg = null
                            actionExecutionLog = null

                            scope.launch(Dispatchers.IO) {
                                try {
                                    val body = mapOf("prompt" to userPrompt, "mode" to selectedMode)
                                    // Backend mounts AI routes at /api/ai, so with
                                    // NetworkClient's base (.../api) this hits
                                    // https://wren-server-production.up.railway.app/api/ai/chat
                                    val response = NetworkClient.post("/ai/chat", body)
                                    if (response.isSuccessful) {
                                        val resBodyStr = response.body?.string()
                                        val data = Gson().fromJson(resBodyStr, ChatResponse::class.java)
                                        sessionManager.userCredits = data.remainingCredits
                                        withContext(Dispatchers.Main) {
                                            aiLogs = aiLogs + ("Asistente" to data.response)
                                            data.actions?.let { if (it.isNotEmpty()) proposedActions = it }
                                            isLoading = false
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            errorMsg = "El servidor no pudo procesar la solicitud. Inténtalo de nuevo."
                                            isLoading = false
                                        }
                                    }
                                } catch (e: java.io.IOException) {
                                    // Real network failure — show it honestly instead
                                    // of faking a response and charging a credit for it.
                                    withContext(Dispatchers.Main) {
                                        errorMsg = "No se pudo conectar con Wren AI. Comprueba tu conexión e inténtalo de nuevo."
                                        isLoading = false
                                    }
                                } catch (t: Throwable) {
                                    withContext(Dispatchers.Main) {
                                        errorMsg = "Ocurrió un error inesperado."
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                ModeSelectorRow(selectedMode = selectedMode, onSelect = { selectedMode = it })

                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    items(aiLogs) { (sender, msg) ->
                        ChatBubble(sender = sender, message = msg)
                    }

                    if (proposedActions.isNotEmpty()) {
                        item {
                            BentoActionsPanel(
                                actions = proposedActions,
                                expandedIndices = expandedDiffIndices,
                                acceptedIndices = acceptedActionIndices,
                                onToggleExpand = { idx ->
                                    expandedDiffIndices = if (expandedDiffIndices.contains(idx)) {
                                        expandedDiffIndices - idx
                                    } else {
                                        expandedDiffIndices + idx
                                    }
                                },
                                onReject = { idx ->
                                    proposedActions = proposedActions.filterIndexed { i, _ -> i != idx }
                                },
                                onAccept = { idx, action ->
                                    isLoading = true
                                    scope.launch(Dispatchers.IO) {
                                        try {
                                            val body = mapOf(
                                                "action" to action.type,
                                                "path" to action.path,
                                                "content" to action.content
                                            )
                                            val response = NetworkClient.post("/ai/agent/execute", body)
                                            if (response.isSuccessful) {
                                                val resStr = response.body?.string()
                                                val remaining = Gson().fromJson(resStr, ChatResponse::class.java).remainingCredits
                                                sessionManager.userCredits = remaining.coerceAtLeast(0)
                                                withContext(Dispatchers.Main) {
                                                    acceptedActionIndices = acceptedActionIndices + idx
                                                    actionExecutionLog = "Aplicado: ${action.path ?: "acción"}"
                                                    isLoading = false
                                                }
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    errorMsg = "No se pudo aplicar el cambio. Inténtalo de nuevo."
                                                    isLoading = false
                                                }
                                            }
                                        } catch (e: java.io.IOException) {
                                            withContext(Dispatchers.Main) {
                                                errorMsg = "Sin conexión — el cambio no se aplicó."
                                                isLoading = false
                                            }
                                        }
                                    }
                                },
                                onDismissAll = { proposedActions = emptyList() },
                                onDone = { proposedActions = emptyList(); acceptedActionIndices = emptySet() }
                            )
                        }
                    }

                    actionExecutionLog?.let { log ->
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(TerminalGreen.copy(alpha = 0.10f))
                                    .border(1.dp, TerminalGreen.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = TerminalGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(log, color = TerminalGreen, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeSelectorRow(selectedMode: String, onSelect: (String) -> Unit) {
    val modes = listOf(
        Triple("Chat", "chat", Icons.Filled.Chat),
        Triple("Planner", "planificador", Icons.Filled.Assignment),
        Triple("Composer", "editor", Icons.Filled.Edit),
        Triple("Terminal", "terminal", Icons.Filled.Terminal)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(modes) { (label, key, icon) ->
            val isActive = selectedMode == key
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isActive) ElectricCyan.copy(alpha = 0.15f) else SecondaryCard.copy(alpha = 0.4f))
                    .border(1.dp, if (isActive) ElectricCyan.copy(alpha = 0.4f) else BorderGray, RoundedCornerShape(14.dp))
                    .clickable { onSelect(key) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, contentDescription = null, tint = if (isActive) ElectricCyan else TextMuted, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(label, color = if (isActive) ElectricCyan else TextMuted, fontSize = 12.5.sp, fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun ChatBubble(sender: String, message: String) {
    val isUser = sender == "Usuario"
    GlassPanel(
        modifier = Modifier.fillMaxWidth(),
        accent = if (isUser) ElectricCyan else EditorYellow
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 12.dp, start = 14.dp, end = 14.dp, bottom = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background((if (isUser) ElectricCyan else EditorYellow).copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isUser) Icons.Filled.Person else Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = if (isUser) ElectricCyan else EditorYellow,
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isUser) "Tú" else "Wren AI",
                color = if (isUser) ElectricCyan else EditorYellow,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Text(
            text = message,
            color = TextLight,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
        )
    }
}

@Composable
private fun BentoActionsPanel(
    actions: List<ChatAction>,
    expandedIndices: Set<Int>,
    acceptedIndices: Set<Int>,
    onToggleExpand: (Int) -> Unit,
    onReject: (Int) -> Unit,
    onAccept: (Int, ChatAction) -> Unit,
    onDismissAll: () -> Unit,
    onDone: () -> Unit
) {
    GlassPanel(modifier = Modifier.fillMaxWidth(), accent = EditorYellow) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = EditorYellow, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cambios propuestos", color = EditorYellow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Wren AI quiere aplicar estos cambios a tu proyecto.",
                color = TextMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Bento-style mosaic: each file/command is its own tile rather
            // than a plain stacked list, sized by how much content it holds.
            actions.forEachIndexed { index, action ->
                val isExpanded = expandedIndices.contains(index)
                val isAccepted = acceptedIndices.contains(index)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PrimaryObsidian.copy(alpha = 0.6f))
                        .border(
                            1.dp,
                            if (isAccepted) TerminalGreen.copy(alpha = 0.4f) else BorderGray,
                            RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .background(
                                    if (isAccepted) TerminalGreen.copy(alpha = 0.2f)
                                    else if (action.type == "CREATE_FILE") TerminalGreen.copy(alpha = 0.15f)
                                    else EditorYellow.copy(alpha = 0.15f),
                                    RoundedCornerShape(6.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = if (isAccepted) "APLICADO" else if (action.type == "CREATE_FILE") "CREAR" else "MODIFICAR",
                                color = if (isAccepted || action.type == "CREATE_FILE") TerminalGreen else EditorYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = action.path ?: action.command ?: "comando",
                            color = TextLight,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        if (action.content != null) {
                            IconButton(onClick = { onToggleExpand(index) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    contentDescription = "Ver diff",
                                    tint = TextMuted
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(action.description, color = TextMuted, fontSize = 12.sp)

                    if (isExpanded && action.content != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(SecondaryCard.copy(alpha = 0.7f))
                                .border(1.dp, BorderGray, RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            action.content.split("\n").forEach { line ->
                                val isAdd = line.startsWith("+")
                                val isDel = line.startsWith("-")
                                Text(
                                    text = line,
                                    color = if (isAdd) TerminalGreen else if (isDel) ErrorRed else TextLight,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    if (!isAccepted) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(onClick = { onReject(index) }) {
                                Text("Rechazar", color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { onAccept(index, action) },
                                colors = ButtonDefaults.buttonColors(containerColor = TerminalGreen),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.height(32.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp)
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryObsidian, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aceptar", color = PrimaryObsidian, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismissAll) {
                    Text("Descartar todo", color = TextMuted, fontSize = 13.sp)
                }
                if (acceptedIndices.size == actions.size) {
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Listo", color = PrimaryObsidian, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PromptInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isLoading: Boolean,
    onSend: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SecondaryCard.copy(alpha = 0.75f))
            .border(1.dp, BorderGray, RoundedCornerShape(18.dp))
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = TextLight, fontSize = 14.sp),
            cursorBrush = SolidColor(ElectricCyan),
            decorationBox = { inner ->
                Box(modifier = Modifier.fillMaxWidth()) {
                    if (value.isEmpty()) {
                        Text("Pídele algo a Wren AI Agent…", color = TextMuted, fontSize = 14.sp)
                    }
                    inner()
                }
            },
            modifier = Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 10.dp)
        )
        if (isLoading) {
            Text("…", color = ElectricCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 6.dp))
        } else {
            IconButton(
                onClick = onSend,
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ElectricCyan)
                    .size(40.dp)
            ) {
                Icon(Icons.Filled.Send, contentDescription = "Enviar", tint = PrimaryObsidian, modifier = Modifier.size(18.dp))
            }
        }
    }
}
