package com.wren.ide.core.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProjectDao {
    @Query("SELECT * FROM local_projects ORDER BY updatedAt DESC")
    suspend fun getAllProjects(): List<ProjectEntity>

    @Query("SELECT * FROM local_projects WHERE id = :id")
    suspend fun getProjectById(id: String): ProjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity)

    @Query("DELETE FROM local_projects WHERE id = :id")
    suspend fun deleteProject(id: String)
}
