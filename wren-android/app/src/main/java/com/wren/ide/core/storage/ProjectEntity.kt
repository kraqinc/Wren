package com.wren.ide.core.storage

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_projects")
data class ProjectEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String
)
