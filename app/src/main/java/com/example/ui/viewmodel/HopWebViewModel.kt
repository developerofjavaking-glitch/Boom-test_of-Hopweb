package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.apk.ApkBuildOptions
import com.example.data.apk.ApkBuildResult
import com.example.data.apk.ApkBuilderEngine
import com.example.data.bundler.WebProjectBundler
import com.example.data.local.AppDatabase
import com.example.data.model.ConsoleLogEntry
import com.example.data.model.LogLevel
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import com.example.data.repository.ProjectRepository
import com.example.ui.editor.CodeHighlighter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    PROJECTS_LIST,
    CODE_EDITOR,
    LIVE_PREVIEW,
    APK_BUILDER
}

enum class ViewportMode(val title: String, val widthDp: Int?) {
    MOBILE("Mobile (360px)", 360),
    TABLET("Tablet (768px)", 768),
    FULL_RESPONSIVE("Responsive Full", null)
}

enum class ApkBuildStatus {
    IDLE,
    BUILDING,
    SUCCESS,
    ERROR
}

class HopWebViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProjectRepository
    val projects: StateFlow<List<ProjectEntity>>

    private val _currentScreen = MutableStateFlow(AppScreen.PROJECTS_LIST)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _activeProject = MutableStateFlow<ProjectEntity?>(null)
    val activeProject: StateFlow<ProjectEntity?> = _activeProject.asStateFlow()

    private val _projectFiles = MutableStateFlow<List<ProjectFileEntity>>(emptyList())
    val projectFiles: StateFlow<List<ProjectFileEntity>> = _projectFiles.asStateFlow()

    private val _activeFile = MutableStateFlow<ProjectFileEntity?>(null)
    val activeFile: StateFlow<ProjectFileEntity?> = _activeFile.asStateFlow()

    private val _editorText = MutableStateFlow(TextFieldValue(""))
    val editorText: StateFlow<TextFieldValue> = _editorText.asStateFlow()

    // Undo / Redo history
    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()

    // DevTools Console
    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    private val _consoleFilter = MutableStateFlow<LogLevel?>(null)
    val consoleFilter: StateFlow<LogLevel?> = _consoleFilter.asStateFlow()

    // Live Preview
    private val _viewportMode = MutableStateFlow(ViewportMode.MOBILE)
    val viewportMode: StateFlow<ViewportMode> = _viewportMode.asStateFlow()

    private val _previewKey = MutableStateFlow(0)
    val previewKey: StateFlow<Int> = _previewKey.asStateFlow()

    private val _isStandaloneMode = MutableStateFlow(false)
    val isStandaloneMode: StateFlow<Boolean> = _isStandaloneMode.asStateFlow()

    // APK Builder
    private val _apkBuildStatus = MutableStateFlow(ApkBuildStatus.IDLE)
    val apkBuildStatus: StateFlow<ApkBuildStatus> = _apkBuildStatus.asStateFlow()

    private val _apkBuildProgress = MutableStateFlow(Pair(0, "Ready"))
    val apkBuildProgress: StateFlow<Pair<Int, String>> = _apkBuildProgress.asStateFlow()

    private val _apkBuildResult = MutableStateFlow<ApkBuildResult?>(null)
    val apkBuildResult: StateFlow<ApkBuildResult?> = _apkBuildResult.asStateFlow()

    // Search in code
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var autoSaveJob: Job? = null
    private var filesObservationJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ProjectRepository(db.projectDao())
        projects = repository.allProjects.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun openProject(project: ProjectEntity) {
        _activeProject.value = project
        _consoleLogs.value = emptyList()
        observeProjectFiles(project.id)
        _currentScreen.value = AppScreen.CODE_EDITOR
    }

    private fun observeProjectFiles(projectId: String) {
        filesObservationJob?.cancel()
        filesObservationJob = viewModelScope.launch {
            repository.getFilesForProject(projectId).collect { files ->
                _projectFiles.value = files
                val currentActive = _activeFile.value
                if (currentActive == null || files.none { it.id == currentActive.id }) {
                    val entry = files.find { it.isEntry || it.name.equals("index.html", ignoreCase = true) }
                        ?: files.firstOrNull()
                    entry?.let { selectFile(it) }
                } else {
                    val updated = files.find { it.id == currentActive.id }
                    if (updated != null && updated.content != _editorText.value.text && autoSaveJob?.isActive != true) {
                        _editorText.value = TextFieldValue(updated.content)
                    }
                }
            }
        }
    }

    fun selectFile(file: ProjectFileEntity) {
        saveCurrentEditorContentImmediately()

        _activeFile.value = file
        _editorText.value = TextFieldValue(file.content, TextRange(0))
        undoStack.clear()
        redoStack.clear()
        undoStack.add(file.content)
    }

    fun onEditorTextChange(newVal: TextFieldValue) {
        val oldText = _editorText.value.text
        _editorText.value = newVal

        if (newVal.text != oldText) {
            if (undoStack.isEmpty() || Math.abs(newVal.text.length - (undoStack.lastOrNull()?.length ?: 0)) > 5) {
                undoStack.add(newVal.text)
                if (undoStack.size > 50) undoStack.removeAt(0)
                redoStack.clear()
            }
            triggerDebouncedAutoSave()
        }
    }

    private fun triggerDebouncedAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(350)
            saveCurrentEditorContentImmediately()
        }
    }

    fun saveCurrentEditorContentImmediately() {
        val file = _activeFile.value ?: return
        val project = _activeProject.value ?: return
        val text = _editorText.value.text
        if (text != file.content) {
            viewModelScope.launch {
                repository.updateFileContent(file.id, project.id, text)
            }
        }
    }

    fun insertSymbol(symbol: String) {
        val current = _editorText.value
        val selection = current.selection
        val text = current.text
        val newText = text.substring(0, selection.start) + symbol + text.substring(selection.end)
        val newCursor = selection.start + symbol.length

        onEditorTextChange(
            TextFieldValue(
                text = newText,
                selection = TextRange(newCursor)
            )
        )
    }

    fun undo() {
        if (undoStack.size > 1) {
            val current = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            val previous = undoStack.last()
            _editorText.value = TextFieldValue(previous, TextRange(previous.length))
            saveCurrentEditorContentImmediately()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(next)
            _editorText.value = TextFieldValue(next, TextRange(next.length))
            saveCurrentEditorContentImmediately()
        }
    }

    fun formatCurrentCode() {
        val file = _activeFile.value ?: return
        val formatted = CodeHighlighter.formatCode(_editorText.value.text, file.extension)
        if (formatted != _editorText.value.text) {
            onEditorTextChange(TextFieldValue(formatted, TextRange(0)))
            saveCurrentEditorContentImmediately()
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearching.value = !_isSearching.value
        if (!_isSearching.value) _searchQuery.value = ""
    }

    fun replaceSearchMatch(replacement: String, replaceAll: Boolean = false) {
        val query = _searchQuery.value
        if (query.isEmpty()) return
        val current = _editorText.value.text
        val newText = if (replaceAll) {
            current.replace(query, replacement)
        } else {
            current.replaceFirst(query, replacement)
        }
        onEditorTextChange(TextFieldValue(newText))
        saveCurrentEditorContentImmediately()
    }

    fun createNewProject(
        name: String,
        description: String,
        category: String,
        iconType: String,
        accentColor: Long,
        packageName: String = "com.ropweb.talha.aijavadevs"
    ) {
        viewModelScope.launch {
            val id = repository.createProject(
                name = name,
                description = description,
                category = category,
                iconType = iconType,
                accentColor = accentColor,
                packageName = packageName
            )
            val project = repository.getProjectByIdSync(id)
            project?.let { openProject(it) }
        }
    }

    fun addNewFile(fileName: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            val defaultContent = when (fileName.substringAfterLast(".").lowercase()) {
                "html" -> "<!DOCTYPE html>\n<html>\n<head>\n  <title>$fileName</title>\n</head>\n<body>\n  <h1>$fileName</h1>\n</body>\n</html>"
                "css" -> "/* Stylesheet: $fileName */\n"
                "js" -> "// Script: $fileName\nconsole.log('$fileName loaded');\n"
                else -> ""
            }
            val newFile = repository.addFile(project.id, fileName, defaultContent)
            selectFile(newFile)
        }
    }

    fun deleteFile(fileId: String) {
        val project = _activeProject.value ?: return
        viewModelScope.launch {
            repository.deleteFile(fileId, project.id)
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            if (_activeProject.value?.id == projectId) {
                _activeProject.value = null
                _currentScreen.value = AppScreen.PROJECTS_LIST
            }
        }
    }

    // DevTools & Preview
    fun addConsoleLog(level: LogLevel, message: String, line: Int = 0) {
        val entry = ConsoleLogEntry(
            level = level,
            message = message,
            lineNumber = if (line > 0) line else null,
            timestamp = System.currentTimeMillis()
        )
        val list = _consoleLogs.value.toMutableList()
        list.add(entry)
        if (list.size > 200) list.removeAt(0)
        _consoleLogs.value = list
    }

    fun clearConsole() {
        _consoleLogs.value = emptyList()
    }

    fun setConsoleFilter(filter: LogLevel?) {
        _consoleFilter.value = filter
    }

    fun setViewportMode(mode: ViewportMode) {
        _viewportMode.value = mode
    }

    fun refreshPreview() {
        _previewKey.value++
    }

    fun setStandaloneMode(enabled: Boolean) {
        _isStandaloneMode.value = enabled
    }

    fun getBundledHtmlForPreview(): String {
        return WebProjectBundler.bundleProjectForPreview(_projectFiles.value, injectDevTools = true)
    }

    // APK Generation with direct database sync
    fun startBuildApk(options: ApkBuildOptions) {
        val project = _activeProject.value ?: return
        _apkBuildStatus.value = ApkBuildStatus.BUILDING
        _apkBuildProgress.value = Pair(1, "Starting RopeWeb APK packager...")

        viewModelScope.launch {
            // Save any active editor text first
            saveCurrentEditorContentImmediately()

            // Fetch latest files directly from database to guarantee fresh files
            val freshFiles = repository.getFilesForProjectSync(project.id)
            val filesToPackage = if (freshFiles.isNotEmpty()) freshFiles else _projectFiles.value

            val result = ApkBuilderEngine.buildApk(
                context = getApplication(),
                project = project,
                files = filesToPackage,
                options = options
            ) { step, total, message ->
                _apkBuildProgress.value = Pair(step, message)
            }

            _apkBuildResult.value = result
            _apkBuildStatus.value = if (result.isSuccess) ApkBuildStatus.SUCCESS else ApkBuildStatus.ERROR
        }
    }
}
