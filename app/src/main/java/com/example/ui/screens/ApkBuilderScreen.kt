package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.InstallMobile
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.apk.ApkBuildOptions
import com.example.data.apk.ApkBuilderEngine
import com.example.data.model.ProjectEntity
import com.example.ui.viewmodel.ApkBuildStatus
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HopWebViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApkBuilderScreen(
    viewModel: HopWebViewModel,
    project: ProjectEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    BackHandler {
        viewModel.navigateTo(AppScreen.CODE_EDITOR)
    }

    val apkBuildStatus by viewModel.apkBuildStatus.collectAsStateWithLifecycle()
    val apkBuildProgress by viewModel.apkBuildProgress.collectAsStateWithLifecycle()
    val apkBuildResult by viewModel.apkBuildResult.collectAsStateWithLifecycle()
    val projectFiles by viewModel.projectFiles.collectAsStateWithLifecycle()

    var appName by remember { mutableStateOf(project.name) }
    var packageName by remember {
        mutableStateOf(
            if (project.packageName.isNotBlank()) project.packageName
            else "com.hopweb.app." + project.name.lowercase().replace("[^a-z0-9]".toRegex(), "")
        )
    }
    var versionName by remember { mutableStateOf(project.versionName) }
    var versionCode by remember { mutableIntStateOf(project.versionCode) }
    var orientation by remember { mutableStateOf(project.orientation) } // unspecified, portrait, landscape
    var enableFullscreen by remember { mutableStateOf(project.enableFullscreen) }
    var enableOfflineCache by remember { mutableStateOf(project.enableOfflineCache) }
    var includeInternet by remember { mutableStateOf(true) }
    var includeCamera by remember { mutableStateOf(false) }
    var includeStorage by remember { mutableStateOf(false) }

    var showBuildTerminalLogs by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CODE_EDITOR) },
                        modifier = Modifier.testTag("btn_apk_back")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFFF1F5F9)
                        )
                    }
                },
                title = {
                    Column {
                        Text(
                            text = "Web to APK Creator",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFFF1F5F9)
                        )
                        Text(
                            text = "Offline & Online Package Studio",
                            fontSize = 11.sp,
                            color = Color(0xFF10B981)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.navigateTo(AppScreen.LIVE_PREVIEW) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Test Preview", tint = Color(0xFF38BDF8))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF090D16),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // App Banner Preview Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF131B2E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(project.accentColor).copy(alpha = 0.25f),
                            modifier = Modifier.size(60.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Android,
                                    contentDescription = null,
                                    tint = Color(project.accentColor),
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appName.ifEmpty { "My App" },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9)
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = packageName,
                                fontSize = 12.sp,
                                color = Color(0xFF38BDF8),
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "v$versionName (Build $versionCode) • ${projectFiles.size} Project Files",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Build Status / Success Card
            if (apkBuildStatus == ApkBuildStatus.SUCCESS && apkBuildResult != null) {
                item {
                    val result = apkBuildResult!!
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF064E3B).copy(alpha = 0.6f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF10B981), RoundedCornerShape(20.dp))
                            .testTag("card_apk_success")
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("APK Generated Successfully!", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFF1F5F9))
                                    Text(
                                        "${result.apkFile?.name ?: "app.apk"} (${result.fileSizeBytes / 1024} KB)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF6EE7B7)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Install APK button
                                Button(
                                    onClick = {
                                        result.apkFile?.let { file ->
                                            ApkBuilderEngine.installApk(context, file)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color(0xFF0F172A)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_install_apk")
                                ) {
                                    Icon(Icons.Default.InstallMobile, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Install APK", fontWeight = FontWeight.Bold)
                                }

                                // Share APK button
                                OutlinedButton(
                                    onClick = {
                                        result.apkFile?.let { file ->
                                            ApkBuilderEngine.shareFile(context, file, "application/vnd.android.package-archive", "Share APK")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("btn_share_apk")
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Share APK")
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Export ZIP
                                OutlinedButton(
                                    onClick = {
                                        result.zipFile?.let { zip ->
                                            ApkBuilderEngine.shareFile(context, zip, "application/zip", "Export Project ZIP")
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Export ZIP", fontSize = 12.sp)
                                }

                                // Test in Standalone Mode
                                Button(
                                    onClick = {
                                        viewModel.refreshPreview()
                                        viewModel.navigateTo(AppScreen.LIVE_PREVIEW)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = Color(0xFFF1F5F9)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Run Preview", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            } else if (apkBuildStatus == ApkBuildStatus.BUILDING) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("card_apk_building")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Step ${apkBuildProgress.first} of 5",
                                color = Color(0xFF00E5FF),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = apkBuildProgress.second,
                                color = Color(0xFFF1F5F9),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { apkBuildProgress.first / 5f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = Color(0xFF00E5FF),
                                trackColor = Color(0xFF1E293B)
                            )
                        }
                    }
                }
            } else if (apkBuildStatus == ApkBuildStatus.ERROR) {
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF36151E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFF43F5E), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Build Error", fontWeight = FontWeight.Bold, color = Color(0xFFF1F5F9))
                                Text(apkBuildResult?.errorMessage ?: "Failed to generate package", color = Color(0xFFFDA4AF), fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Build Action Button
            item {
                Button(
                    onClick = {
                        val options = ApkBuildOptions(
                            appName = appName.ifBlank { project.name },
                            packageName = packageName.ifBlank { "com.hopweb.app.project" },
                            versionName = versionName.ifBlank { "1.0.0" },
                            versionCode = versionCode,
                            orientation = orientation,
                            enableFullscreen = enableFullscreen,
                            enableOfflineCache = enableOfflineCache,
                            includeInternetPermission = includeInternet,
                            includeCameraPermission = includeCamera,
                            includeStoragePermission = includeStorage
                        )
                        viewModel.startBuildApk(options)
                    },
                    enabled = apkBuildStatus != ApkBuildStatus.BUILDING,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                        contentColor = Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("btn_build_apk_now")
                ) {
                    Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (apkBuildStatus == ApkBuildStatus.BUILDING) "Building APK..." else "BUILD & PACK APK NOW",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Package Configuration Settings
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PACKAGE CONFIGURATION",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8),
                            letterSpacing = 1.sp
                        )

                        OutlinedTextField(
                            value = appName,
                            onValueChange = { appName = it },
                            label = { Text("Application Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_apk_app_name")
                        )

                        OutlinedTextField(
                            value = packageName,
                            onValueChange = { packageName = it },
                            label = { Text("Android Package ID") },
                            placeholder = { Text("com.company.app") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("input_apk_package_id")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = versionName,
                                onValueChange = { versionName = it },
                                label = { Text("Version Name") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = versionCode.toString(),
                                onValueChange = { versionCode = it.toIntOrNull() ?: 1 },
                                label = { Text("Version Code") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Orientation Selector
                        Column {
                            Text("Screen Orientation", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(
                                    "unspecified" to "Auto Sensor",
                                    "portrait" to "Portrait",
                                    "landscape" to "Landscape"
                                ).forEach { (id, label) ->
                                    FilterChip(
                                        selected = orientation == id,
                                        onClick = { orientation = id },
                                        label = { Text(label, fontSize = 11.sp) },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF38BDF8),
                                            selectedLabelColor = Color(0xFF0F172A)
                                        )
                                    )
                                }
                            }
                        }

                        // Fullscreen mode toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Immersive Fullscreen", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFF1F5F9))
                                Text("Hides status bar and navigation for games & kiosks", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = enableFullscreen,
                                onCheckedChange = { enableFullscreen = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF38BDF8))
                            )
                        }

                        // Offline mode toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Offline Caching Mode", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFFF1F5F9))
                                Text("Bundles 100% of project files into APK assets", fontSize = 11.sp, color = Color(0xFF94A3B8))
                            }
                            Switch(
                                checked = enableOfflineCache,
                                onCheckedChange = { enableOfflineCache = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF10B981))
                            )
                        }
                    }
                }
            }

            // Android Permissions Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF131B2E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "APP PERMISSIONS IN MANIFEST",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Internet Access (CDN, APIs)", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                            Switch(checked = includeInternet, onCheckedChange = { includeInternet = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Camera Access (Webcam, AR)", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                            Switch(checked = includeCamera, onCheckedChange = { includeCamera = it })
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Storage Access (File Picker)", fontSize = 13.sp, color = Color(0xFFE2E8F0))
                            Switch(checked = includeStorage, onCheckedChange = { includeStorage = it })
                        }
                    }
                }
            }

            // Terminal Logs Dropdown
            apkBuildResult?.let { result ->
                if (result.logOutput.isNotEmpty()) {
                    item {
                        Surface(
                            onClick = { showBuildTerminalLogs = !showBuildTerminalLogs },
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Terminal, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("View APK Build Logs (${result.logOutput.size} entries)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                            }
                        }

                        AnimatedVisibility(visible = showBuildTerminalLogs) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF05070D),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    result.logOutput.forEach { logLine ->
                                        Text(
                                            text = logLine,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = if (logLine.contains("ERROR")) Color(0xFFF43F5E) else Color(0xFF94A3B8),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
