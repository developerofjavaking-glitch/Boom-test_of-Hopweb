package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.JsResult
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.ConsoleLogEntry
import com.example.data.model.LogLevel
import com.example.data.model.ProjectEntity
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HopWebViewModel
import com.example.ui.viewmodel.ViewportMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SandboxPreviewScreen(
    viewModel: HopWebViewModel,
    project: ProjectEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val previewKey by viewModel.previewKey.collectAsStateWithLifecycle()
    val consoleLogs by viewModel.consoleLogs.collectAsStateWithLifecycle()
    val consoleFilter by viewModel.consoleFilter.collectAsStateWithLifecycle()
    val viewportMode by viewModel.viewportMode.collectAsStateWithLifecycle()

    var isFullscreen by remember { mutableStateOf(false) }
    var isDevToolsExpanded by remember { mutableStateOf(false) }
    var devToolsTabIndex by remember { mutableIntStateOf(0) }
    var replCommand by remember { mutableStateOf("") }
    var showViewportMenu by remember { mutableStateOf(false) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val errorCount = remember(consoleLogs) {
        consoleLogs.count { it.level == LogLevel.ERROR }
    }

    BackHandler {
        if (isFullscreen) {
            isFullscreen = false
        } else if (isDevToolsExpanded) {
            isDevToolsExpanded = false
        } else {
            viewModel.navigateTo(AppScreen.CODE_EDITOR)
        }
    }

    val bundledHtml = remember(previewKey) {
        viewModel.getBundledHtmlForPreview()
    }

    Scaffold(
        topBar = {
            if (!isFullscreen) {
                TopAppBar(
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.CODE_EDITOR) },
                            modifier = Modifier.testTag("btn_preview_back")
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF0F172A)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Live Sandbox Preview",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = project.name,
                                fontSize = 12.sp,
                                color = Color(0xFF2563EB)
                            )
                        }
                    },
                    actions = {
                        // Viewport Selector
                        Box {
                            IconButton(onClick = { showViewportMenu = true }) {
                                Icon(Icons.Default.Devices, contentDescription = "Viewport", tint = Color(0xFF475569))
                            }
                            DropdownMenu(
                                expanded = showViewportMenu,
                                onDismissRequest = { showViewportMenu = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                ViewportMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.title,
                                                color = if (viewportMode == mode) Color(0xFF2563EB) else Color(0xFF0F172A),
                                                fontWeight = if (viewportMode == mode) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        onClick = {
                                            viewModel.setViewportMode(mode)
                                            showViewportMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        // Reload / Refresh
                        IconButton(
                            onClick = {
                                viewModel.refreshPreview()
                                webViewRef?.reload()
                            },
                            modifier = Modifier.testTag("btn_refresh_preview")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF475569))
                        }

                        // DevTools Console Toggle
                        IconButton(
                            onClick = { isDevToolsExpanded = !isDevToolsExpanded },
                            modifier = Modifier.testTag("btn_toggle_console")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (errorCount > 0) {
                                        Badge(
                                            containerColor = Color(0xFFE11D48),
                                            contentColor = Color.White
                                        ) {
                                            Text("$errorCount")
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Default.Terminal,
                                    contentDescription = "DevTools",
                                    tint = if (isDevToolsExpanded) Color(0xFF2563EB) else Color(0xFF475569)
                                )
                            }
                        }

                        // Fullscreen Preview Toggle
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color(0xFF475569))
                        }

                        // Build APK shortcut
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.APK_BUILDER) },
                            modifier = Modifier.testTag("btn_preview_to_apk")
                        ) {
                            Icon(Icons.Default.Android, contentDescription = "Make APK", tint = Color(0xFF16A34A))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            }
        },
        containerColor = Color(0xFFF1F5F9),
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isFullscreen) PaddingValues(0.dp) else innerPadding)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Sandbox WebView Container
                Box(
                    modifier = Modifier
                        .weight(if (isDevToolsExpanded) 0.55f else 1f)
                        .fillMaxWidth()
                        .background(Color(0xFFF1F5F9)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerModifier = when (val width = viewportMode.widthDp) {
                        null -> Modifier.fillMaxSize()
                        else -> Modifier
                            .width(width.dp)
                            .fillMaxHeight()
                            .padding(vertical = if (isFullscreen) 0.dp else 12.dp)
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                    }

                    AndroidView(
                        factory = { ctx ->
                            createConfiguredWebView(ctx, viewModel).also { wv ->
                                webViewRef = wv
                                wv.loadDataWithBaseURL(
                                    "https://hopweb.local/",
                                    bundledHtml,
                                    "text/html",
                                    "UTF-8",
                                    null
                                )
                            }
                        },
                        update = { wv ->
                            webViewRef = wv
                            wv.loadDataWithBaseURL(
                                "https://hopweb.local/",
                                bundledHtml,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        },
                        modifier = containerModifier.testTag("sandbox_webview")
                    )
                }

                // DevTools Sheet (Light Mode)
                if (isDevToolsExpanded && !isFullscreen) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // DevTools Header / Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFF8FAFC))
                                    .border(1.dp, Color(0xFFE2E8F0))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DevToolsTabButtonLight(
                                        title = "Console (${consoleLogs.size})",
                                        isSelected = devToolsTabIndex == 0,
                                        onClick = { devToolsTabIndex = 0 }
                                    )
                                    DevToolsTabButtonLight(
                                        title = "Bundled Source",
                                        isSelected = devToolsTabIndex == 1,
                                        onClick = { devToolsTabIndex = 1 }
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (devToolsTabIndex == 0) {
                                        IconButton(
                                            onClick = { viewModel.clearConsole() },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(Icons.Default.ClearAll, contentDescription = "Clear Console", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { isDevToolsExpanded = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.FullscreenExit, contentDescription = "Close DevTools", tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (devToolsTabIndex == 0) {
                                ConsoleTabContentLight(
                                    logs = consoleLogs,
                                    activeFilter = consoleFilter,
                                    onFilterChange = { viewModel.setConsoleFilter(it) },
                                    replCommand = replCommand,
                                    onReplCommandChange = { replCommand = it },
                                    onExecuteRepl = { cmd ->
                                        if (cmd.isNotBlank()) {
                                            viewModel.addConsoleLog(LogLevel.INFO, "> $cmd")
                                            webViewRef?.evaluateJavascript(cmd) { result ->
                                                viewModel.addConsoleLog(LogLevel.LOG, "< $result")
                                            }
                                            replCommand = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                SelectionContainer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    LazyColumn {
                                        item {
                                            Text(
                                                text = bundledHtml,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = Color(0xFF334155),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating exit button in Fullscreen mode
            if (isFullscreen) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC0F172A),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                ) {
                    IconButton(onClick = { isFullscreen = false }) {
                        Icon(Icons.Default.FullscreenExit, contentDescription = "Exit Fullscreen", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun DevToolsTabButtonLight(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color(0xFF2563EB) else Color(0xFF64748B)
        )
    }
}

@Composable
fun ConsoleTabContentLight(
    logs: List<ConsoleLogEntry>,
    activeFilter: LogLevel?,
    onFilterChange: (LogLevel?) -> Unit,
    replCommand: String,
    onReplCommandChange: (String) -> Unit,
    onExecuteRepl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, activeFilter) {
        if (activeFilter == null) logs
        else logs.filter { it.level == activeFilter }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Filter pills row (Light Mode)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .border(1.dp, Color(0xFFE2E8F0))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChipLight(label = "All", isSelected = activeFilter == null, onClick = { onFilterChange(null) })
            FilterChipLight(label = "Log", isSelected = activeFilter == LogLevel.LOG, color = Color(0xFF2563EB), onClick = { onFilterChange(LogLevel.LOG) })
            FilterChipLight(label = "Warn", isSelected = activeFilter == LogLevel.WARN, color = Color(0xFFD97706), onClick = { onFilterChange(LogLevel.WARN) })
            FilterChipLight(label = "Error", isSelected = activeFilter == LogLevel.ERROR, color = Color(0xFFE11D48), onClick = { onFilterChange(LogLevel.ERROR) })
        }

        // Log Items List (Light Mode)
        SelectionContainer(modifier = Modifier.weight(1f)) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No console logs yet. Interact with the preview to test.", color = Color(0xFF94A3B8), fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val (bg, badgeBg, badgeColor, label) = when (log.level) {
                            LogLevel.LOG -> Quad(Color(0xFFF8FAFC), Color(0xFFEFF6FF), Color(0xFF2563EB), "LOG")
                            LogLevel.INFO -> Quad(Color(0xFFF8FAFC), Color(0xFFF3E8FF), Color(0xFF7C3AED), "INFO")
                            LogLevel.WARN -> Quad(Color(0xFFFFFBEB), Color(0xFFFEF3C7), Color(0xFFD97706), "WARN")
                            LogLevel.ERROR -> Quad(Color(0xFFFFF1F2), Color(0xFFFFE4E6), Color(0xFFE11D48), "ERR")
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = bg,
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeBg
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.message,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF0F172A),
                                    lineHeight = 16.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                log.lineNumber?.let { line ->
                                    Text(
                                        text = ":$line",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive JS REPL Bar (Light Mode)
        Surface(
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(">", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = replCommand,
                    onValueChange = onReplCommandChange,
                    placeholder = { Text("eval JS (e.g. document.title, alert(1))", fontSize = 12.sp, color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFF2563EB),
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .testTag("input_repl_command")
                )
                IconButton(
                    onClick = { onExecuteRepl(replCommand) },
                    modifier = Modifier.testTag("btn_eval_repl")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run JS", tint = Color(0xFF2563EB), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@Composable
fun FilterChipLight(
    label: String,
    isSelected: Boolean,
    color: Color = Color(0xFF475569),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) color else Color(0xFFCBD5E1))
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color(0xFF475569),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
fun createConfiguredWebView(context: Context, viewModel: HopWebViewModel): WebView {
    return WebView(context).apply {
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            allowFileAccess = true
            allowContentAccess = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
        }

        addJavascriptInterface(object : Any() {
            @JavascriptInterface
            fun onConsoleMessage(level: String, message: String, line: Int) {
                val logLevel = when (level.uppercase()) {
                    "INFO" -> LogLevel.INFO
                    "WARN" -> LogLevel.WARN
                    "ERROR" -> LogLevel.ERROR
                    else -> LogLevel.LOG
                }
                viewModel.addConsoleLog(logLevel, message, line)
            }
        }, "HopWebBridge")

        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let {
                    val lvl = when (it.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> LogLevel.ERROR
                        ConsoleMessage.MessageLevel.WARNING -> LogLevel.WARN
                        ConsoleMessage.MessageLevel.TIP -> LogLevel.INFO
                        else -> LogLevel.LOG
                    }
                    viewModel.addConsoleLog(lvl, it.message(), it.lineNumber())
                }
                return true
            }

            override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
                Toast.makeText(context, message ?: "", Toast.LENGTH_SHORT).show()
                result?.confirm()
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                error?.let {
                    viewModel.addConsoleLog(LogLevel.ERROR, "Network/Resource Error: ${it.description}")
                }
            }
        }
    }
}
