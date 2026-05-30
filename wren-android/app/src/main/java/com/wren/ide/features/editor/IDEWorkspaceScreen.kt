package com.wren.ide.features.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    var isFileTreeExpanded by remember { mutableStateOf(true) }
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "WREN",
                            color = TextLight,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        selectedProject?.let {
                            Text(
                                "| ${it.name}",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavToAI) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = "AI Agent",
                            tint = ElectricCyan
                        )
                    }
                    IconButton(onClick = onNavToCredits) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.MonetizationOn,
                                contentDescription = "Credits",
                                tint = EditorYellow
                            )
                            Text(
                                "${sessionManager.userCredits}",
                                color = EditorYellow,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                    if (sessionManager.userRole == "OWNER" || sessionManager.userRole == "SUPER_ADMIN") {
                        IconButton(onClick = onNavToOwner) {
                            Icon(
                                Icons.Filled.AdminPanelSettings,
                                contentDescription = "Owner Dashboard",
                                tint = TerminalGreen
                            )
                        }
                    }
                    IconButton(onClick = { onLogout() }) {
                        Icon(
                            Icons.Filled.Logout,
                            contentDescription = "Logout",
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = com.wren.ide.core.theme.SecondaryCard)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(PrimaryObsidian)
        ) {
            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isFileTreeExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.35f)
                            .background(com.wren.ide.core.theme.SecondaryCard)
                            .border(1.dp, BorderGray)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PROYECTOS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showNewProjectDialog = true },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Add,
                                    contentDescription = "Add Project",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .height(100.dp)
                                .padding(horizontal = 8.dp)
                        ) {
                            items(projects) { project ->
                                Text(
                                    text = project.name,
                                    color = if (selectedProject?.id == project.id) ElectricCyan else TextLight,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedProject?.id == project.id) FontWeight.Bold else FontWeight.Normal,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedProject = project }
                                        .padding(vertical = 4.dp, horizontal = 6.dp)
                                )
                            }
                        }

                        Divider(color = BorderGray, thickness = 1.dp)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "ARCHIVOS",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                            selectedProject?.let {
                                IconButton(
                                    onClick = { showNewFileDialog = true },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.CreateNewFolder,
                                        contentDescription = "Add File",
                                        tint = ElectricCyan,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        ) {
                            items(files) { file ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (file.is_directory == 0) {
                                                selectedFile = file
                                                codeContent = TextFieldValue(file.content ?: "")
                                            }
                                        }
                                        .padding(vertical = 4.dp, horizontal = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = if (file.is_directory == 1) Icons.Filled.Folder else Icons.Filled.Description,
                                        contentDescription = null,
                                        tint = if (file.is_directory == 1) ElectricCyan else TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = file.name,
                                        color = if (selectedFile?.id == file.id) ElectricCyan else TextLight,
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(12.dp)
                        .background(BorderGray)
                        .clickable { isFileTreeExpanded = !isFileTreeExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isFileTreeExpanded) Icons.Filled.ChevronLeft else Icons.Filled.ChevronRight,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(10.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    selectedFile?.let { activeFile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(com.wren.ide.core.theme.SecondaryCard)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = activeFile.path,
                                color = TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = {
                                    selectedProject?.let { p ->
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
                                                    }
                                                }
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Save,
                                    contentDescription = "Save file",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(PrimaryObsidian)
                        ) {
                            BasicTextField(
                                value = codeContent,
                                onValueChange = { codeContent = it },
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
                                    .padding(8.dp)
                            )
                        }
                    } ?: Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(PrimaryObsidian),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Crea o selecciona un archivo para empezar a programar.",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(PrimaryObsidian)
                    .border(1.dp, BorderGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.wren.ide.core.theme.SecondaryCard)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "TERMINAL",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { terminalOutput = "wren-terminal-session reset: SUCCESS\n$ " },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "Reset terminal",
                            tint = TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    item {
                        Text(
                            text = terminalOutput,
                            color = TerminalGreen,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(com.wren.ide.core.theme.SecondaryCard)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("$ ", color = TerminalGreen, fontFamily = FontFamily.Monospace, fontSize = 13.sp)
                    BasicTextField(
                        value = terminalInput,
                        onValueChange = { terminalInput = it },
                        textStyle = TextStyle(
                            color = TextLight,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        cursorBrush = SolidColor(ElectricCyan),
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
                            .clickable { }
                    )
                    IconButton(
                        onClick = {
                            if (terminalInput.isBlank()) return@IconButton
                            val command = terminalInput.trim()
                            terminalOutput += "$command\n"

                            val simulatedResponse = when {
                                command == "wren build" || command == "wren compile" -> {
                                    "Wren Build Engine v1.0...\n> Compiling sources...\n> Task :compile SUCCESS\n> Task :assemble SUCCESS\n\nBUILD SUCCESSFUL in 4s\nAPK generated: wren-app.apk (1.8 MB)\n$ "
                                }
                                command.startsWith("git ") -> {
                                    "git branch main: local tracking active.\nEverything up-to-date.\n$ "
                                }
                                command == "clear" -> {
                                    ""
                                }
                                else -> {
                                    "wren-shell: command not found: $command. Try 'wren build' or 'git status'.\n$ "
                                }
                            }

                            terminalOutput = if (command == "clear") "$ " else terminalOutput + simulatedResponse
                            terminalInput = ""
                        },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Filled.Send,
                            contentDescription = "Execute",
                            tint = ElectricCyan,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
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
                                        isLoading = false
                                    }
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
            containerColor = com.wren.ide.core.theme.SecondaryCard
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
                                                isLoading = false
                                            }
                                        }
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
            containerColor = com.wren.ide.core.theme.SecondaryCard
        )
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
                    withStyle(
                        style = SpanStyle(
                            color = TextMuted,
                            fontStyle = FontStyle.Italic
                        )
                    ) {
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
                    withStyle(
                        style = SpanStyle(
                            color = EditorYellow,
                            fontWeight = FontWeight.Bold
                        )
                    ) {
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