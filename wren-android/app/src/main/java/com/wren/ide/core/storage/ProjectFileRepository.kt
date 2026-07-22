package com.wren.ide.core.storage

import java.util.UUID

/**
 * Punto único de escritura para archivos de proyecto. Todo lo que antes
 * llamaba a [FileDao] directamente debería pasar por aquí en su lugar --
 * así cada archivo vive tanto en Room (para que la app lo lea rápido) como
 * en almacenamiento real (para que el usuario lo vea en cualquier
 * explorador de archivos, en
 * /storage/emulated/0/wren/wren_internal/wren/wren-proyect/<proyecto>/).
 *
 * Si algo falla al escribir en disco real (ej. falta el permiso de "acceso
 * a todos los archivos" en Android 11+), el archivo igual se guarda en Room
 * -- la app sigue funcionando, solo no aparece reflejado en el explorador
 * de archivos hasta que el usuario conceda el permiso.
 */
class ProjectFileRepository(
    private val fileDao: FileDao,
    private val projectDao: ProjectDao
) {

    /** Construye la ruta relativa completa de un archivo a partir de su cadena de padres. */
    private suspend fun relativePathOf(file: FileEntity): String {
        val segments = mutableListOf(file.name)
        var parentId = file.parentId
        while (parentId != null) {
            val parent = fileDao.getFileById(parentId) ?: break
            segments.add(0, parent.name)
            parentId = parent.parentId
        }
        return segments.joinToString("/")
    }

    suspend fun createFile(
        projectId: String,
        name: String,
        parentId: String?,
        isDirectory: Boolean,
        initialContent: String? = ""
    ): FileEntity {
        val entity = FileEntity(
            id = UUID.randomUUID().toString(),
            projectId = projectId,
            name = name,
            path = name,
            isDirectory = if (isDirectory) 1 else 0,
            content = if (isDirectory) null else (initialContent ?: ""),
            parentId = parentId
        )
        fileDao.insertFile(entity)
        mirrorToDisk(entity)
        return entity
    }

    suspend fun updateFileContent(fileId: String, content: String) {
        fileDao.updateFileContent(fileId, content)
        val updated = fileDao.getFileById(fileId) ?: return
        mirrorToDisk(updated)
    }

    suspend fun deleteFile(fileId: String) {
        val file = fileDao.getFileById(fileId) ?: return
        val project = projectDao.getProjectById(file.projectId)
        fileDao.deleteFile(fileId)
        if (project != null) {
            try {
                val relPath = relativePathOf(file)
                WrenFileStorage.delete(project.name, relPath)
            } catch (_: Exception) {
                // Sin permiso de almacenamiento todavía -- Room ya se actualizó, seguimos.
            }
        }
    }

    private suspend fun mirrorToDisk(file: FileEntity) {
        val project = projectDao.getProjectById(file.projectId) ?: return
        if (!WrenFileStorage.hasAllFilesAccess()) return // se sincroniza después, al conceder el permiso

        try {
            val relPath = relativePathOf(file)
            if (file.isDirectory == 1) {
                WrenFileStorage.createDirectory(project.name, relPath)
            } else {
                WrenFileStorage.writeFile(project.name, relPath, file.content ?: "")
            }
        } catch (_: Exception) {
            // No tumbar el guardado en Room por un fallo de disco real -- se reintenta
            // la próxima vez que el usuario guarde o abra el proyecto.
        }
    }

    /** Vuelve a espejar TODOS los archivos de un proyecto a disco real -- útil
     * justo después de que el usuario concede el permiso de almacenamiento,
     * para no perder lo que se guardó mientras no lo tenía. */
    suspend fun resyncProjectToDisk(projectId: String) {
        if (!WrenFileStorage.hasAllFilesAccess()) return
        val files = fileDao.getFilesForProject(projectId)
        files.forEach { mirrorToDisk(it) }
    }
}
