package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import com.example.ui.components.CodeEditorView
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HopWebViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: HopWebViewModel,
    project: ProjectEntity,
    modifier: Modifier = Modifier
) {
    BackHandler {
        viewModel.saveCurrentEditorContentImmediately()
        viewModel.navigateTo(AppScreen.PROJECTS_LIST)
    }

    val projectFiles by viewModel.projectFiles.collectAsStateWithLifecycle()
    val activeFile by viewModel.activeFile.collectAsStateWithLifecycle()
    val editorText by viewModel.editorText.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var showNewFileDialog by remember { mutableStateOf(false) }
    var fileToDelete by remember { mutableStateOf<ProjectFileEntity?>(null) }
    var replaceText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentEditorContentImmediately()
                            viewModel.navigateTo(AppScreen.PROJECTS_LIST)
                        },
                        modifier = Modifier.testTag("btn_editor_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to Projects",
                            tint = Color(0xFF0F172A)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = project.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = activeFile?.name ?: "No file",
                            fontSize = 12.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                },
                actions = {
                    // Undo
                    IconButton(onClick = { viewModel.undo() }) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo", tint = Color(0xFF475569))
                    }
                    // Redo
                    IconButton(onClick = { viewModel.redo() }) {
                        Icon(Icons.AutoMirrored.Filled.Redo, contentDescription = "Redo", tint = Color(0xFF475569))
                    }
                    // Search in file
                    IconButton(onClick = { viewModel.toggleSearch() }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (isSearching) Color(0xFF2563EB) else Color(0xFF475569)
                        )
                    }
                    // Format code
                    IconButton(onClick = { viewModel.formatCurrentCode() }) {
                        Icon(Icons.Default.FormatAlignLeft, contentDescription = "Format Code", tint = Color(0xFF475569))
                    }
                    // Make APK button
                    IconButton(
                        onClick = {
                            viewModel.saveCurrentEditorContentImmediately()
                            viewModel.navigateTo(AppScreen.APK_BUILDER)
                        },
                        modifier = Modifier.testTag("btn_goto_apk")
                    ) {
                        Icon(Icons.Default.Android, contentDescription = "Build APK", tint = Color(0xFF16A34A))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    viewModel.saveCurrentEditorContentImmediately()
                    viewModel.refreshPreview()
                    viewModel.navigateTo(AppScreen.LIVE_PREVIEW)
                },
                containerColor = Color(0xFF16A34A),
                contentColor = Color.White,
                shape = CircleShape,
                modifier = Modifier
                    .padding(bottom = 52.dp)
                    .testTag("fab_run_preview")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Run Code")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("RUN", fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Replace Bar (Light Mode)
            if (isSearching) {
                Surface(
                    color = Color(0xFFF1F5F9),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { viewModel.setSearchQuery(it) },
                                placeholder = { Text("Find...", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                            OutlinedTextField(
                                value = replaceText,
                                onValueChange = { replaceText = it },
                                placeholder = { Text("Replace with...", fontSize = 12.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            )
                            IconButton(onClick = { viewModel.toggleSearch() }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color(0xFF64748B))
                            }
                        }
                        Row(
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            TextButton(
                                onClick = { viewModel.replaceSearchMatch(replaceText, replaceAll = false) }
                            ) {
                                Text("Replace Next", fontSize = 12.sp, color = Color(0xFF2563EB))
                            }
                            TextButton(
                                onClick = { viewModel.replaceSearchMatch(replaceText, replaceAll = true) }
                            ) {
                                Text("Replace All", fontSize = 12.sp, color = Color(0xFF2563EB))
                            }
                        }
                    }
                }
            }

            // File Tabs Bar (Light Mode)
            Surface(
                color = Color(0xFFF1F5F9),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(projectFiles, key = { it.id }) { file ->
                        val isSelected = file.id == activeFile?.id
                        val extColor = when (file.extension.lowercase()) {
                            "html" -> Color(0xFFE11D48)
                            "css" -> Color(0xFF2563EB)
                            "js" -> Color(0xFFD97706)
                            else -> Color(0xFF0284C7)
                        }

                        Surface(
                            onClick = { viewModel.selectFile(file) },
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color.White else Color.Transparent,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)) else null,
                            shadowElevation = if (isSelected) 1.dp else 0.dp,
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .height(34.dp)
                                .testTag("file_tab_${file.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 10.dp, end = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(extColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = file.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color(0xFF0F172A) else Color(0xFF64748B)
                                )

                                if (!file.isEntry && projectFiles.size > 1) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    IconButton(
                                        onClick = { fileToDelete = file },
                                        modifier = Modifier.size(20.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Close File",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(12.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Add File Button
                    item {
                        IconButton(
                            onClick = { showNewFileDialog = true },
                            modifier = Modifier
                                .size(34.dp)
                                .padding(start = 4.dp)
                                .testTag("btn_add_file")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add File", tint = Color(0xFF2563EB))
                        }
                    }
                }
            }

            // Code Editor
            if (activeFile != null) {
                CodeEditorView(
                    textFieldValue = editorText,
                    onValueChange = { viewModel.onEditorTextChange(it) },
                    fileExtension = activeFile?.extension ?: "txt",
                    onInsertSymbol = { sym -> viewModel.insertSymbol(sym) },
                    modifier = Modifier.weight(1f)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Select or create a file to start editing", color = Color(0xFF94A3B8))
                }
            }
        }
    }

    if (showNewFileDialog) {
        CreateFileDialogLight(
            onDismiss = { showNewFileDialog = false },
            onCreate = { fileName ->
                showNewFileDialog = false
                viewModel.addNewFile(fileName)
            }
        )
    }

    fileToDelete?.let { file ->
        AlertDialog(
            onDismissRequest = { fileToDelete = null },
            title = { Text("Delete ${file.name}?", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to remove this file from the project?", color = Color(0xFF64748B)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteFile(file.id)
                        fileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE11D48))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { fileToDelete = null }) {
                    Text("Cancel", color = Color(0xFF64748B))
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun CreateFileDialogLight(
    onDismiss: () -> Unit,
    onCreate: (fileName: String) -> Unit
) {
    var fileName by remember { mutableStateOf("") }
    val suggestedExtensions = listOf(".html", ".css", ".js", ".json")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add File to Project", fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("File Name") },
                    placeholder = { Text("e.g. page2.html, app.css") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2563EB),
                        focusedLabelColor = Color(0xFF2563EB)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_filename")
                )

                Text("Quick Extensions:", fontSize = 12.sp, color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    suggestedExtensions.forEach { ext ->
                        Surface(
                            onClick = {
                                val base = fileName.substringBeforeLast(".")
                                fileName = if (base.isEmpty()) "file$ext" else "$base$ext"
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFEFF6FF),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE))
                        ) {
                            Text(
                                text = ext,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2563EB),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (fileName.isNotBlank()) {
                        onCreate(fileName.trim())
                    }
                },
                enabled = fileName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.testTag("btn_confirm_add_file")
            ) {
                Text("Add File", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White
    )
}
