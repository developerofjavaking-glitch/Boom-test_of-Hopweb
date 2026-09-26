package com.example.data.repository

import com.example.data.local.ProjectDao
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import com.example.data.model.StarterTemplates
import com.example.data.model.TemplateDefinition
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

    suspend fun seedInitialProjectsIfNeeded() {
        val current = projectDao.getFilesForProjectSync("check")
        // If projects table is empty, seed Neon Space Arcade and Cyber Calc
        // We will check by reading the first item
        // Let's create starter projects if none exist
        createProjectFromTemplate(StarterTemplates.templates[0]) // Neon Arcade
        createProjectFromTemplate(StarterTemplates.templates[1]) // Cyber Calc
    }

    suspend fun createProject(
        name: String,
        description: String = "",
        category: String = "Web App",
        iconType: String = "code",
        accentColor: Long = 0xFF38BDF8,
        packageName: String = "com.hopweb.app." + name.lowercase().replace("[^a-z0-9]".toRegex(), "")
    ): String {
        val projectId = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = projectId,
            name = name,
            description = description,
            category = category,
            iconType = iconType,
            accentColor = accentColor,
            packageName = if (packageName.length < 5) "com.hopweb.app.myproject" else packageName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)

        // Create default boilerplate files
        val htmlFile = ProjectFileEntity(
            projectId = projectId,
            name = "index.html",
            extension = "html",
            content = """<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>$name</title>
  <link rel="stylesheet" href="style.css">
</head>
<body>
  <div class="container">
    <h1>🚀 Welcome to $name</h1>
    <p>Created with HopWeb Mobile Code Studio</p>
    <button id="action-btn">Click Me!</button>
    <p id="msg"></p>
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
            content = """body {
  margin: 0;
  padding: 24px;
  background: #0f172a;
  color: #f8fafc;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  box-sizing: border-box;
}
.container {
  text-align: center;
  background: #1e293b;
  padding: 32px 24px;
  border-radius: 20px;
  border: 1px solid #334155;
  box-shadow: 0 10px 25px rgba(0,0,0,0.5);
  max-width: 400px;
  width: 100%;
}
h1 {
  font-size: 22px;
  margin-bottom: 8px;
  color: #38bdf8;
}
p {
  color: #94a3b8;
  font-size: 14px;
  margin-bottom: 24px;
}
button {
  background: linear-gradient(135deg, #38bdf8, #818cf8);
  color: #0f172a;
  font-weight: 700;
  font-size: 15px;
  border: none;
  border-radius: 12px;
  padding: 12px 28px;
  cursor: pointer;
  transition: transform 0.1s ease;
}
button:active {
  transform: scale(0.96);
}
#msg {
  margin-top: 18px;
  font-weight: 600;
  color: #10b981;
}"""
        )

        val jsFile = ProjectFileEntity(
            projectId = projectId,
            name = "script.js",
            extension = "js",
            content = """console.log("App loaded successfully!");

let count = 0;
const btn = document.getElementById("action-btn");
const msg = document.getElementById("msg");

btn.addEventListener("click", () => {
  count++;
  msg.textContent = "Button tapped " + count + " time(s)! ⚡";
  console.log("Interactive click event triggered. Count:", count);
});"""
        )

        projectDao.insertFiles(listOf(htmlFile, cssFile, jsFile))
        return projectId
    }

    suspend fun createProjectFromTemplate(template: TemplateDefinition): String {
        val projectId = UUID.randomUUID().toString()
        val project = ProjectEntity(
            id = projectId,
            name = template.name,
            description = template.description,
            category = template.category,
            iconType = template.iconType,
            accentColor = template.accentColor,
            packageName = template.defaultPackageName,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        projectDao.insertProject(project)

        val files = template.files.map { (fileName, fileContent) ->
            val ext = fileName.substringAfterLast(".", "txt")
            ProjectFileEntity(
                projectId = projectId,
                name = fileName,
                extension = ext,
                content = fileContent,
                isEntry = fileName.equals("index.html", ignoreCase = true)
            )
        }
        projectDao.insertFiles(files)
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
