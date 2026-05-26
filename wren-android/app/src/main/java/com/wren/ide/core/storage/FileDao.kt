package com.wren.ide.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface FileDao {
    @Query("SELECT * FROM local_files WHERE projectId = :projectId")
    suspend fun getFilesForProject(projectId: String): List<FileEntity>

    @Query("SELECT * FROM local_files WHERE id = :id")
    suspend fun getFileById(id: String): FileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity)

    @Query("UPDATE local_files SET content = :content WHERE id = :fileId")
    suspend fun updateFileContent(fileId: String, content: String)

    @Query("DELETE FROM local_files WHERE id = :fileId")
    suspend fun deleteFile(fileId: String)
}
