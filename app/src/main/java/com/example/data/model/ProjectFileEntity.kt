package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "project_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"]), Index(value = ["projectId", "name"], unique = true)]
)
data class ProjectFileEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val projectId: String,
    val name: String, // e.g. "index.html", "style.css", "script.js"
    val extension: String = "html", // html, css, js, json, svg, txt
    val content: String = "",
    val isEntry: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)
