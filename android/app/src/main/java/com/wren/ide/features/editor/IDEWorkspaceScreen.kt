package com.wren.ide.features.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wren.ide.core.network.FileItem
import com.wren.ide.core.network.NetworkClient
import com.wren.ide.core.network.Project
import com.wren.ide.core.network.ProjectFilesResponse
import com.wren.ide.core.network.ProjectListResponse
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IDEWorkspaceScreen(
    sessionManager: SessionManager,
    onNavToAI: () -> Unit,
    onNavToCredits: () -> Unit,
    onNavToOwner: () -> Unit,
    onLogout: () -> Unit
) {
    val scope = rememberCoroutineScope()

    var projects by remember { mutableStateOf<List<Project>>(emptyList()) }
    var selectedProject by remember { mutableStateOf<Project?>(null) }
    var files by remember { mutableStateOf<List<FileItem>>(emptyList()) }
    var selectedFile by remember { mutableStateOf<FileItem?>(null) }
    var codeContent by remember { mutableStateOf(TextFieldValue("")) }
    var isDirty by remember { mutableStateOf(false) }

    var showFileExplorer by remember { mutableStateOf(false) }
    var showTerminal by remember { mutableStateOf(false) }
    var terminalOutput by remember { mutableStateOf("wren-terminal-session init: OK\n$ ") }
    var terminalInput by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showNewFileDialog by remember { mutableStateOf(false) }
    var newProjectName by remember { mutableStateOf("") }
    var newFileName by remember { mutableStateOf("") }
    var isNewFileDirectory by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            withContext(Dispatchers.IO) {
                val response = NetworkClient.get("/projects")
                if (response.isSuccessful) {
                    val body = response.body?.string()
                    val pResponse = Gson().fromJson(body, ProjectListResponse::class.java)
                    withContext(Dispatchers.Main) {
                        projects = pResponse.projects
                        if (projects.isNotEmpty()) {
                            selectedProject = projects.first()
                        } else {
                            // No projects yet — open the explorer so the user
                            // isn't dropped onto a blank, unexplained screen.
                            showFileExplorer = true
                        }
                        isLoading = false
                    }
                } else {
                    withContext(Dispatchers.Main) { isLoading = false }
                }
            }
        } catch (_: Throwable) {
            isLoading = false
        }
    }

    LaunchedEffect(selectedProject) {
        selectedProject?.let { project ->
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val response = NetworkClient.get("/projects/${project.id}/files")
                    if (response.isSuccessful) {
                        val body = response.body?.string()
                        val fResponse = Gson().fromJson(body, ProjectFilesResponse::class.java)
                        withContext(Dispatchers.Main) {
                            files = fResponse.files
                            selectedFile = files.find { it.is_directory == 0 }
                            selectedFile?.let {
                                codeContent = TextFieldValue(it.content ?: "")
                                isDirty = false
                            }
                            isLoading = false
                        }
                    } else {
                        withContext(Dispatchers.Main) { isLoading = false }
                    }
                }
            } catch (_: Throwable) {
                isLoading = false
            }
        }
    }

    fun saveActiveFile() {
        val p = selectedProject ?: return
        val activeFile = selectedFile ?: return
        scope.launch(Dispatchers.IO) {
            try {
                val body = mapOf("content" to codeContent.text)
                val res = NetworkClient.put("/projects/${p.id}/files/${activeFile.id}", body)
                if (res.isSuccessful) {
                    val updatedFiles = files.map {
                        if (it.id == activeFile.id) it.copy(content = codeContent.text) else it
                    }
                    withContext(Dispatchers.Main) {
                        files = updatedFiles
                        isDirty = false
                    }
                }
            } catch (_: Exception) {
                // Save failures are non-fatal — the user's edits remain in the
                // editor buffer and they can retry.
            }
        }
    }

    Scaffold(
        topBar = {
            WorkspaceTopBar(
                projectName = selectedProject?.name,
                fileName = selectedFile?.name,
                isDirty = isDirty,
                credits = sessionManager.userCredits,
                showOwnerAction = sessionManager.userRole == "OWNER" || sessionManager.userRole == "SUPER_ADMIN",
                onOpenExplorer = { showFileExplorer = true },
                onOpenTerminal = { showTerminal = true },
                onSave = { saveActiveFile() },
                onNavToAI = onNavToAI,
                onNavToCredits = onNavToCredits,
                onNavToOwner = onNavToOwner,
                onLogout = onLogout
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PrimaryObsidian)
        ) {
            // --- Full-screen editor: the primary surface on mobile, not a
            // pane squeezed next to a permanent file tree. ---
            selectedFile?.let {
                BasicTextField(
                    value = codeContent,
                    onValueChange = {
                        codeContent = it
                        isDirty = true
                    },
                    textStyle = TextStyle(
                        color = TextLight,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(ElectricCyan),
                    visualTransformation = VisualTransformation { text ->
                        val annotated = highlightKotlinSyntax(text.text)
                        TransformedText(annotated, OffsetMapping.Identity)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .padding(bottom = 72.dp) // room for the Ask AI bar
                )
            } ?: EmptyEditorState(
                hasProject = selectedProject != null,
                onOpenExplorer = { showFileExplorer = true }
            )

            // --- Persistent "Ask AI" bar, Cursor-style, always reachable
            // without leaving the editor. ---
            AskAiBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                onClick = onNavToAI
            )

            if (isLoading) {
                Text(
                    text = "Cargando…",
                    color = ElectricCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 6.dp)
                )
            }
        }
    }

    // --- File explorer as a bottom sheet, not a permanent 35%-width sidebar
    // stealing space from the editor on a phone screen. ---
    if (showFileExplorer) {
        ModalBottomSheet(
            onDismissRequest = { showFileExplorer = false },
            containerColor = SecondaryCard
        ) {
            FileExplorerSheet(
                projects = projects,
                selectedProject = selectedProject,
                files = files,
                selectedFile = selectedFile,
                onSelectProject = { selectedProject = it },
                onSelectFile = { file ->
                    if (file.is_directory == 0) {
                        selectedFile = file
                        codeContent = TextFieldValue(file.content ?: "")
                        isDirty = false
                        showFileExplorer = false
                    }
                },
                onNewProject = { showNewProjectDialog = true },
                onNewFile = { showNewFileDialog = true }
            )
        }
    }

    // --- Terminal as a bottom sheet the user summons, instead of a fixed
    // 150dp strip permanently eating vertical space on every screen. ---
    if (showTerminal) {
        ModalBottomSheet(
            onDismissRequest = { showTerminal = false },
            containerColor = PrimaryObsidian
        ) {
            TerminalSheet(
                output = terminalOutput,
                input = terminalInput,
                onInputChange = { terminalInput = it },
                onReset = { terminalOutput = "wren-terminal-session reset: SUCCESS\n$ " },
                onExecute = {
                    if (terminalInput.isNotBlank()) {
                        val command = terminalInput.trim()
                        val simulatedResponse = when {
                            command == "wren build" || command == "wren compile" ->
                                "Wren Build Engine v1.0...\n> Compiling sources...\n> Task :compile SUCCESS\n> Task :assemble SUCCESS\n\nBUILD SUCCESSFUL in 4s\nAPK generated: wren-app.apk (1.8 MB)\n$ "
                            command.startsWith("git ") ->
                                "git branch main: local tracking active.\nEverything up-to-date.\n$ "
                            command == "clear" -> ""
                            else -> "wren-shell: command not found: $command. Prueba 'wren build' o 'git status'.\n$ "
                        }
                        terminalOutput = if (command == "clear") "$ " else "$terminalOutput$command\n$simulatedResponse"
                        terminalInput = ""
                    }
                }
            )
        }
    }

    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Nuevo Proyecto", color = TextLight) },
            text = {
                OutlinedTextField(
                    value = newProjectName,
                    onValueChange = { newProjectName = it },
                    label = { Text("Nombre del Proyecto", color = TextMuted) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextLight,
                        unfocusedTextColor = TextLight,
                        focusedBorderColor = ElectricCyan,
                        unfocusedBorderColor = BorderGray
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newProjectName.isBlank()) return@TextButton
                        showNewProjectDialog = false
                        isLoading = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val body = mapOf("name" to newProjectName, "description" to "Proyecto inicial")
                                val response = NetworkClient.post("/projects", body)
                                if (response.isSuccessful) {
                                    val responseBodyStr = response.body?.string()
                                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                                    val map: Map<String, Any> = Gson().fromJson(responseBodyStr, mapType)
                                    val projectMap = map["project"] as Map<*, *>
                                    val newProj = Project(
                                        id = projectMap["id"] as String,
                                        name = projectMap["name"] as String,
                                        description = projectMap["description"] as String,
                                        created_at = projectMap["created_at"] as String,
                                        updated_at = projectMap["updated_at"] as String
                                    )
                                    withContext(Dispatchers.Main) {
                                        projects = projects + newProj
                                        selectedProject = newProj
                                        newProjectName = ""
                                        isLoading = false
                                    }
                                } else {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            } catch (_: Exception) {
                                withContext(Dispatchers.Main) { isLoading = false }
                            }
                        }
                    }
                ) {
                    Text("Crear", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = SecondaryCard
        )
    }

    if (showNewFileDialog) {
        AlertDialog(
            onDismissRequest = { showNewFileDialog = false },
            title = { Text("Crear Elemento", color = TextLight) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("Nombre del Archivo/Directorio", color = TextMuted) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextLight,
                            unfocusedTextColor = TextLight,
                            focusedBorderColor = ElectricCyan,
                            unfocusedBorderColor = BorderGray
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isNewFileDirectory,
                            onCheckedChange = { isNewFileDirectory = it },
                            colors = CheckboxDefaults.colors(checkedColor = ElectricCyan)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("¿Es un directorio?", color = TextLight, fontSize = 14.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFileName.isBlank()) return@TextButton
                        showNewFileDialog = false
                        selectedProject?.let { p ->
                            isLoading = true
                            scope.launch(Dispatchers.IO) {
                                try {
                                    val body = mapOf(
                                        "name" to newFileName,
                                        "path" to newFileName,
                                        "isDirectory" to isNewFileDirectory,
                                        "content" to ""
                                    )
                                    val response = NetworkClient.post("/projects/${p.id}/files", body)
                                    if (response.isSuccessful) {
                                        val fResponse = NetworkClient.get("/projects/${p.id}/files")
                                        if (fResponse.isSuccessful) {
                                            val b = fResponse.body?.string()
                                            val filesData = Gson().fromJson(b, ProjectFilesResponse::class.java)
                                            withContext(Dispatchers.Main) {
                                                files = filesData.files
                                                newFileName = ""
                                                isNewFileDirectory = false
                                                isLoading = false
                                            }
                                        }
                                    } else {
                                        withContext(Dispatchers.Main) { isLoading = false }
                                    }
                                } catch (_: Exception) {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        }
                    }
                ) {
                    Text("Crear", color = ElectricCyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFileDialog = false }) {
                    Text("Cancelar", color = TextMuted)
                }
            },
            containerColor = SecondaryCard
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkspaceTopBar(
    projectName: String?,
    fileName: String?,
    isDirty: Boolean,
    credits: Int,
    showOwnerAction: Boolean,
    onOpenExplorer: () -> Unit,
    onOpenTerminal: () -> Unit,
    onSave: () -> Unit,
    onNavToAI: () -> Unit,
    onNavToCredits: () -> Unit,
    onNavToOwner: () -> Unit,
    onLogout: () -> Unit
) {
    Column {
        TopAppBar(
            title = {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Code, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("WREN", color = TextLight, fontSize = 17.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    }
                    if (fileName != null) {
                        Text(
                            text = (if (isDirty) "• " else "") + fileName,
                            color = if (isDirty) EditorYellow else TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1
                        )
                    } else if (projectName != null) {
                        Text(projectName, color = TextMuted, fontSize = 11.sp, maxLines = 1)
                    }
                }
            },
            navigationIcon = {
                IconButton(onClick = onOpenExplorer) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = "Explorador de archivos", tint = TextLight)
                }
            },
            actions = {
                if (fileName != null) {
                    IconButton(onClick = onSave, enabled = isDirty) {
                        Icon(
                            Icons.Filled.Save,
                            contentDescription = "Guardar",
                            tint = if (isDirty) ElectricCyan else TextMuted.copy(alpha = 0.4f)
                        )
                    }
                }
                IconButton(onClick = onOpenTerminal) {
                    Icon(Icons.Filled.Terminal, contentDescription = "Terminal", tint = TextLight)
                }
                IconButton(onClick = onNavToAI) {
                    Icon(Icons.Filled.AutoAwesome, contentDescription = "AI Agent", tint = ElectricCyan)
                }
                IconButton(onClick = onNavToCredits) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.MonetizationOn, contentDescription = "Créditos", tint = EditorYellow, modifier = Modifier.size(18.dp))
                        Text("$credits", color = EditorYellow, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 2.dp))
                    }
                }
                if (showOwnerAction) {
                    IconButton(onClick = onNavToOwner) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Panel de administración", tint = TerminalGreen)
                    }
                }
                IconButton(onClick = onLogout) {
                    Icon(Icons.Filled.Logout, contentDescription = "Cerrar sesión", tint = ErrorRed)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = SecondaryCard)
        )
        Divider(color = BorderGray, thickness = 1.dp)
    }
}

@Composable
private fun AskAiBar(modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(SecondaryCard, RoundedCornerShape(14.dp))
            .border(1.dp, BorderGray, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text("Preguntar a Wren AI sobre este archivo…", color = TextMuted, fontSize = 13.sp)
    }
}

@Composable
private fun EmptyEditorState(hasProject: Boolean, onOpenExplorer: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.Description, contentDescription = null, tint = TextMuted, modifier = Modifier.size(36.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (hasProject) "Selecciona un archivo para empezar a programar" else "Crea o abre un proyecto para empezar",
                color = TextMuted,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onOpenExplorer) {
                Text(if (hasProject) "Abrir explorador de archivos" else "Ir a proyectos", color = ElectricCyan)
            }
        }
    }
}

@Composable
private fun FileExplorerSheet(
    projects: List<Project>,
    selectedProject: Project?,
    files: List<FileItem>,
    selectedFile: FileItem?,
    onSelectProject: (Project) -> Unit,
    onSelectFile: (FileItem) -> Unit,
    onNewProject: () -> Unit,
    onNewFile: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 320.dp, max = 560.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("PROYECTOS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            IconButton(onClick = onNewProject, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Add, contentDescription = "Nuevo proyecto", tint = ElectricCyan, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (projects.isEmpty()) {
            Text("Aún no tienes proyectos.", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(bottom = 12.dp))
        } else {
            LazyColumn(modifier = Modifier.heightIn(max = 120.dp)) {
                items(projects) { project ->
                    val isSelected = selectedProject?.id == project.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectProject(project) }
                            .background(
                                if (isSelected) ElectricCyan.copy(alpha = 0.08f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 8.dp, horizontal = 8.dp)
                    ) {
                        Icon(Icons.Filled.Folder, contentDescription = null, tint = if (isSelected) ElectricCyan else TextMuted, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = project.name,
                            color = if (isSelected) ElectricCyan else TextLight,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        Divider(color = BorderGray, thickness = 1.dp, modifier = Modifier.padding(vertical = 12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("ARCHIVOS", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            if (selectedProject != null) {
                IconButton(onClick = onNewFile, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Filled.CreateNewFolder, contentDescription = "Nuevo archivo", tint = ElectricCyan, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedProject == null) {
            Text("Selecciona un proyecto para ver sus archivos.", color = TextMuted, fontSize = 12.sp)
        } else if (files.isEmpty()) {
            Text("Este proyecto todavía no tiene archivos.", color = TextMuted, fontSize = 12.sp)
        } else {
            LazyColumn(modifier = Modifier.weight(1f, fill = false).heightIn(max = 280.dp)) {
                items(files) { file ->
                    val isSelected = selectedFile?.id == file.id
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectFile(file) }
                            .background(
                                if (isSelected) ElectricCyan.copy(alpha = 0.08f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 9.dp, horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (file.is_directory == 1) Icons.Filled.Folder else Icons.Filled.Description,
                            contentDescription = null,
                            tint = if (file.is_directory == 1) ElectricCyan else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = file.name, color = if (isSelected) ElectricCyan else TextLight, fontSize = 13.sp, maxLines = 1)
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun TerminalSheet(
    output: String,
    input: String,
    onInputChange: (String) -> Unit,
    onReset: () -> Unit,
    onExecute: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Terminal, contentDescription = null, tint = TextMuted, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("TERMINAL", color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
            }
            IconButton(onClick = onReset, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Refresh, contentDescription = "Reiniciar terminal", tint = TextMuted, modifier = Modifier.size(15.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(SecondaryCard, RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            item {
                Text(text = output, color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 12.sp, lineHeight = 17.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SecondaryCard, RoundedCornerShape(10.dp))
                .border(1.dp, BorderGray, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("$ ", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
            BasicTextField(
                value = input,
                onValueChange = onInputChange,
                textStyle = TextStyle(color = TextLight, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                cursorBrush = SolidColor(ElectricCyan),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp, top = 8.dp, bottom = 8.dp)
            )
            IconButton(onClick = onExecute, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Send, contentDescription = "Ejecutar", tint = ElectricCyan, modifier = Modifier.size(16.dp))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

fun highlightKotlinSyntax(text: String): AnnotatedString {
    return buildAnnotatedString {
        val keywords = setOf(
            "package", "import", "class", "interface", "fun", "val", "var",
            "if", "else", "for", "while", "return", "try", "catch", "throw",
            "null", "true", "false", "object", "private", "public", "protected"
        )

        var currentIndex = 0
        val tokenRegex = Regex("""(//.*)|(".*?")|(\d+)|([a-zA-Z_][a-zA-Z0-9_]*)|([^\s])""")

        tokenRegex.findAll(text).forEach { result ->
            val match = result.value
            val start = result.range.first

            if (start > currentIndex) {
                append(text.substring(currentIndex, start))
            }

            when {
                match.startsWith("//") -> {
                    withStyle(style = SpanStyle(color = TextMuted, fontStyle = FontStyle.Italic)) {
                        append(match)
                    }
                }

                match.startsWith("\"") && match.endsWith("\"") -> {
                    withStyle(style = SpanStyle(color = TerminalGreen)) {
                        append(match)
                    }
                }

                match.all { it.isDigit() } -> {
                    withStyle(style = SpanStyle(color = ElectricCyan)) {
                        append(match)
                    }
                }

                keywords.contains(match) -> {
                    withStyle(style = SpanStyle(color = EditorYellow, fontWeight = FontWeight.Bold)) {
                        append(match)
                    }
                }

                else -> {
                    append(match)
                }
            }
            currentIndex = result.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}
