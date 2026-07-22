package com.wren.ide.core.storage

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_files",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FileEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val name: String,
    val path: String,
    val isDirectory: Int, // 0 file, 1 dir
    val content: String?,
    val parentId: String?
)
