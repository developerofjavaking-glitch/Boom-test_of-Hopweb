package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ProjectRepository(private val projectDao: ProjectDao) {

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    fun getProjectById(id: String): Flow<ProjectEntity?> = projectDao.getProjectById(id)

    suspend fun getProjectByIdSync(id: String): ProjectEntity? = projectDao.getProjectByIdSync(id)

    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>> =
        projectDao.getFilesForProject(projectId)

    suspend fun getFilesForProjectSync(projectId: String): List<ProjectFileEntity> =
        projectDao.getFilesForProjectSync(projectId)

    suspend fun createProject(
        name: String,
        description: String = "",
        category: String = "Web App",
        iconType: String = "code",
        accentColor: Long = 0xFF2563EB,
        packageName: String = "com.hopweb.app." + name.lowercase().replace("[^a-z0-9]".toRegex(), "")
    ): String {
        val projectId = UUID.randomUUID().toString()
        val safePackage = if (packageName.length < 5) "com.hopweb.app.myproject" else packageName

        val project = ProjectEntity(
            id = projectId,
            name = name,
            description = description,
            category = category,
            iconType = iconType,
            accentColor = accentColor,
            packageName = safePackage,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)

        // Clean starter template: index.html, style.css, script.js
        val htmlFile = ProjectFileEntity(
            projectId = projectId,
            name = "index.html",
            extension = "html",
            content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=no">
  <title>$name</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="app-card">
    <div class="icon">🚀</div>
    <h1>$name</h1>
    <p>Build, test, and convert this project to an APK!</p>
    <button id="counter-btn">Taps: <span id="tap-count">0</span></button>
  </div>
  <script src="script.js"></script>
</body>
</html>""",
            isEntry = true
        )

        val cssFile = ProjectFileEntity(
            projectId = projectId,
            name = "style.css",
            extension = "css",
            content = """* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
}
body {
  background: #f8fafc;
  color: #0f172a;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}
.app-card {
  background: #ffffff;
  border-radius: 20px;
  padding: 32px 24px;
  width: 100%;
  max-width: 360px;
  text-align: center;
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.06);
  border: 1px solid #e2e8f0;
}
.icon {
  font-size: 48px;
  margin-bottom: 12px;
}
h1 {
  font-size: 22px;
  color: #2563eb;
  margin-bottom: 8px;
}
p {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.5;
}
button {
  background: #2563eb;
  color: #ffffff;
  border: none;
  font-size: 16px;
  font-weight: 600;
  padding: 12px 28px;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.3);
  transition: transform 0.1s;
}
button:active {
  transform: scale(0.96);
}"""
        )

        val jsFile = ProjectFileEntity(
            projectId = projectId,
            name = "script.js",
            extension = "js",
            content = """console.log("App '$name' loaded successfully!");

let taps = 0;
const btn = document.getElementById("counter-btn");
const tapCount = document.getElementById("tap-count");

btn.addEventListener("click", () => {
  taps++;
  tapCount.textContent = taps;
  console.log("Button tapped. Total taps:", taps);
});"""
        )

        projectDao.insertFiles(listOf(htmlFile, cssFile, jsFile))
        return projectId
    }

    suspend fun updateProject(project: ProjectEntity) {
        projectDao.updateProject(project.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteProject(id: String) {
        projectDao.deleteProjectById(id)
    }

    suspend fun addFile(projectId: String, fileName: String, initialContent: String = ""): ProjectFileEntity {
        val ext = fileName.substringAfterLast(".", "txt").lowercase()
        val file = ProjectFileEntity(
            projectId = projectId,
            name = fileName,
            extension = ext,
            content = initialContent,
            isEntry = fileName.equals("index.html", ignoreCase = true)
        )
        projectDao.insertFile(file)
        projectDao.updateProjectTimestamp(projectId)
        return file
    }

    suspend fun updateFileContent(fileId: String, projectId: String, content: String) {
        projectDao.updateFileContent(fileId, content)
        projectDao.updateProjectTimestamp(projectId)
    }

    suspend fun updateFile(file: ProjectFileEntity) {
        projectDao.updateFile(file.copy(updatedAt = System.currentTimeMillis()))
        projectDao.updateProjectTimestamp(file.projectId)
    }

    suspend fun deleteFile(fileId: String, projectId: String) {
        projectDao.deleteFileById(fileId)
        projectDao.updateProjectTimestamp(projectId)
    }
}
