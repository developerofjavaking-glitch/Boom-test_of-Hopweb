package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ProjectEntity
import com.example.data.model.ProjectFileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun getAllProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    fun getProjectById(id: String): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
    suspend fun getProjectByIdSync(id: String): ProjectEntity?

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY isEntry DESC, name ASC")
    fun getFilesForProject(projectId: String): Flow<List<ProjectFileEntity>>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId ORDER BY isEntry DESC, name ASC")
    suspend fun getFilesForProjectSync(projectId: String): List<ProjectFileEntity>

    @Query("SELECT * FROM project_files WHERE id = :id LIMIT 1")
    fun getFileById(id: String): Flow<ProjectFileEntity?>

    @Query("SELECT * FROM project_files WHERE projectId = :projectId AND name = :name LIMIT 1")
    suspend fun getFileByNameSync(projectId: String, name: String): ProjectFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Update
    suspend fun updateProject(project: ProjectEntity)

    @Query("DELETE FROM projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: ProjectFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<ProjectFileEntity>)

    @Update
    suspend fun updateFile(file: ProjectFileEntity)

    @Query("UPDATE project_files SET content = :content, updatedAt = :updatedAt WHERE id = :fileId")
    suspend fun updateFileContent(fileId: String, content: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE projects SET updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectTimestamp(projectId: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM project_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)
}
