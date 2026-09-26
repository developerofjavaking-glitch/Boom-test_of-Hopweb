package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val description: String = "",
    val category: String = "Web App",
    val iconType: String = "code",
    val accentColor: Long = 0xFF2563EB,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val packageName: String = "com.ropweb.talha.aijavadevs",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val orientation: String = "unspecified", // unspecified, portrait, landscape
    val enableFullscreen: Boolean = false,
    val enableOfflineCache: Boolean = true
)
