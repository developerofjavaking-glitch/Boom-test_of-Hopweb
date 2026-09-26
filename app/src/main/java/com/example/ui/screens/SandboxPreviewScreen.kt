package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                                tint = Color(0xFFF1F5F9)
                            )
                        }
                    },
                    title = {
                        Column {
                            Text(
                                text = "Live Sandbox",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFFF1F5F9)
                            )
                            Text(
                                text = project.name,
                                fontSize = 11.sp,
                                color = Color(0xFF38BDF8)
                            )
                        }
                    },
                    actions = {
                        // Viewport Selector
                        Box {
                            IconButton(onClick = { showViewportMenu = true }) {
                                Icon(Icons.Default.Devices, contentDescription = "Viewport", tint = Color(0xFF94A3B8))
                            }
                            DropdownMenu(
                                expanded = showViewportMenu,
                                onDismissRequest = { showViewportMenu = false },
                                modifier = Modifier.background(Color(0xFF1E293B))
                            ) {
                                ViewportMode.values().forEach { mode ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = mode.title,
                                                color = if (viewportMode == mode) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
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
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color(0xFF94A3B8))
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
                                            containerColor = Color(0xFFF43F5E),
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
                                    tint = if (isDevToolsExpanded) Color(0xFF38BDF8) else Color(0xFF94A3B8)
                                )
                            }
                        }

                        // Fullscreen Preview Toggle
                        IconButton(onClick = { isFullscreen = true }) {
                            Icon(Icons.Default.Fullscreen, contentDescription = "Fullscreen", tint = Color(0xFF94A3B8))
                        }

                        // Build APK shortcut
                        IconButton(
                            onClick = { viewModel.navigateTo(AppScreen.APK_BUILDER) },
                            modifier = Modifier.testTag("btn_preview_to_apk")
                        ) {
                            Icon(Icons.Default.Android, contentDescription = "Make APK", tint = Color(0xFF10B981))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0F172A)
                    )
                )
            }
        },
        containerColor = Color(0xFF090D16),
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
                        .background(Color(0xFF090D16)),
                    contentAlignment = Alignment.Center
                ) {
                    val containerModifier = when (val width = viewportMode.widthDp) {
                        null -> Modifier.fillMaxSize()
                        else -> Modifier
                            .width(width.dp)
                            .fillMaxHeight()
                            .padding(vertical = if (isFullscreen) 0.dp else 12.dp)
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
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
                            // Only reload if HTML changed
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

                // DevTools Sheet (expandable)
                if (isDevToolsExpanded && !isFullscreen) {
                    Surface(
                        color = Color(0xFF111827),
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxWidth()
                            .border(width = 1.dp, color = Color(0xFF1F2937))
                    ) {
                        Column(modifier = Modifier.fillMaxSize()) {
                            // DevTools Header / Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF161F33))
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    DevToolsTabButton(
                                        title = "Console (${consoleLogs.size})",
                                        isSelected = devToolsTabIndex == 0,
                                        onClick = { devToolsTabIndex = 0 }
                                    )
                                    DevToolsTabButton(
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
                                            Icon(Icons.Default.ClearAll, contentDescription = "Clear Console", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(
                                        onClick = { isDevToolsExpanded = false },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.FullscreenExit, contentDescription = "Close DevTools", tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            if (devToolsTabIndex == 0) {
                                // Console Tab Content
                                ConsoleTabContent(
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
                                // Bundled Source Tab Content
                                SelectionContainer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .padding(8.dp)
                                ) {
                                    LazyColumn {
                                        item {
                                            Text(
                                                text = bundledHtml,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color(0xFFCBD5E1),
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

            // Floating exit button when in Fullscreen mode
            if (isFullscreen) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xCC000000),
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
fun DevToolsTabButton(
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
            color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8)
        )
    }
}

@Composable
fun ConsoleTabContent(
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
        // Filter pills row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F172A))
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(label = "All", isSelected = activeFilter == null, onClick = { onFilterChange(null) })
            FilterChip(label = "Log", isSelected = activeFilter == LogLevel.LOG, color = Color(0xFF38BDF8), onClick = { onFilterChange(LogLevel.LOG) })
            FilterChip(label = "Warn", isSelected = activeFilter == LogLevel.WARN, color = Color(0xFFFBBF24), onClick = { onFilterChange(LogLevel.WARN) })
            FilterChip(label = "Error", isSelected = activeFilter == LogLevel.ERROR, color = Color(0xFFF43F5E), onClick = { onFilterChange(LogLevel.ERROR) })
        }

        // Log Items List
        SelectionContainer(modifier = Modifier.weight(1f)) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No console logs yet. Interact with the preview to test.", color = Color(0xFF64748B), fontSize = 12.sp)
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredLogs, key = { it.id }) { log ->
                        val (bg, badgeColor, label) = when (log.level) {
                            LogLevel.LOG -> Triple(Color(0xFF1E293B), Color(0xFF38BDF8), "LOG")
                            LogLevel.INFO -> Triple(Color(0xFF1E293B), Color(0xFF818CF8), "INFO")
                            LogLevel.WARN -> Triple(Color(0xFF332A15), Color(0xFFFBBF24), "WARN")
                            LogLevel.ERROR -> Triple(Color(0xFF36151E), Color(0xFFF43F5E), "ERR")
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = bg,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = badgeColor.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = log.message,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE2E8F0),
                                    lineHeight = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                log.lineNumber?.let { line ->
                                    Text(
                                        text = ":$line",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B),
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Interactive JS REPL Bar
        Surface(
            color = Color(0xFF161F33),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(">", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(6.dp))
                OutlinedTextField(
                    value = replCommand,
                    onValueChange = onReplCommandChange,
                    placeholder = { Text("eval JS (e.g. document.title, alert(1))", fontSize = 11.sp, color = Color(0xFF64748B)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF38BDF8),
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = Color(0xFFF1F5F9),
                        unfocusedTextColor = Color(0xFFF1F5F9)
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
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Run JS", tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun FilterChip(
    label: String,
    isSelected: Boolean,
    color: Color = Color(0xFF94A3B8),
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(6.dp),
        color = if (isSelected) color.copy(alpha = 0.25f) else Color(0xFF1E293B)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) color else Color(0xFF94A3B8),
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

        // Bridge to capture JavaScript console logs from the page
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
