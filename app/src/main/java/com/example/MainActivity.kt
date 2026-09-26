package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ApkBuilderScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ProjectsListScreen
import com.example.ui.screens.SandboxPreviewScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.HopWebViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                HopWebAppRoot()
            }
        }
    }
}

@Composable
fun HopWebAppRoot(
    viewModel: HopWebViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
        color = Color(0xFFF8FAFC)
    ) {
        when (currentScreen) {
            AppScreen.PROJECTS_LIST -> {
                ProjectsListScreen(
                    viewModel = viewModel,
                    projects = projects
                )
            }
            AppScreen.CODE_EDITOR -> {
                activeProject?.let { project ->
                    EditorScreen(
                        viewModel = viewModel,
                        project = project
                    )
                } ?: run {
                    ProjectsListScreen(
                        viewModel = viewModel,
                        projects = projects
                    )
                }
            }
            AppScreen.LIVE_PREVIEW -> {
                activeProject?.let { project ->
                    SandboxPreviewScreen(
                        viewModel = viewModel,
                        project = project
                    )
                } ?: run {
                    ProjectsListScreen(
                        viewModel = viewModel,
                        projects = projects
                    )
                }
            }
            AppScreen.APK_BUILDER -> {
                activeProject?.let { project ->
                    ApkBuilderScreen(
                        viewModel = viewModel,
                        project = project
                    )
                } ?: run {
                    ProjectsListScreen(
                        viewModel = viewModel,
                        projects = projects
                    )
                }
            }
        }
    }
}
