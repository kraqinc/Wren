package com.wren.ide.core.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import java.io.File
import java.io.IOException

/**
 * Espeja cada proyecto como archivos REALES en el almacenamiento del
 * dispositivo, visibles desde cualquier explorador de archivos externo --
 * no solo dentro de la base de datos Room (que es privada de la app y no la
 * ve nadie más).
 *
 * Ruta resultante para un proyecto llamado "MiApp":
 *   /storage/emulated/0/wren/wren_internal/wren/wren-proyect/MiApp/
 *
 * Esto requiere el permiso "Acceso a todos los archivos" (MANAGE_EXTERNAL_STORAGE)
 * en Android 11+ (API 30+) -- no se puede escribir fuera del sandbox de la
 * app ni de las carpetas multimedia estándar sin él. Ver [hasAllFilesAccess]
 * y [requestAllFilesAccess].
 */
object WrenFileStorage {

    private const val ROOT_FOLDER = "wren"
    private const val INTERNAL_FOLDER = "wren_internal"
    private const val APP_FOLDER = "wren"
    private const val PROJECTS_FOLDER = "wren-proyect"

    /**
     * /storage/emulated/0/wren/wren_internal/wren/wren-proyect/
     */
    private fun projectsRoot(): File {
        val base = Environment.getExternalStorageDirectory() // == /storage/emulated/0
        return File(base, "$ROOT_FOLDER/$INTERNAL_FOLDER/$APP_FOLDER/$PROJECTS_FOLDER")
    }

    /** Carpeta real de un proyecto específico, creándola si no existe. */
    fun projectDir(projectName: String): File {
        val safeName = sanitizeName(projectName)
        val dir = File(projectsRoot(), safeName)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    /** Evita que un nombre de proyecto con "/", "..", etc. escape de la carpeta esperada. */
    private fun sanitizeName(name: String): String {
        return name.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .replace("..", "_")
            .ifBlank { "proyecto-sin-nombre" }
    }

    /**
     * Escribe (crea o sobreescribe) un archivo real dentro del proyecto.
     * [relativePath] es la ruta dentro del proyecto, ej. "src/main.kt".
     */
    @Throws(IOException::class)
    fun writeFile(projectName: String, relativePath: String, content: String) {
        val target = File(projectDir(projectName), relativePath)
        target.parentFile?.mkdirs()
        target.writeText(content)
    }

    /** Crea una carpeta real dentro del proyecto (equivalente a `mkdir`). */
    fun createDirectory(projectName: String, relativePath: String): Boolean {
        return File(projectDir(projectName), relativePath).mkdirs()
    }

    /** Lee el contenido real de un archivo del proyecto, o null si no existe. */
    fun readFile(projectName: String, relativePath: String): String? {
        val target = File(projectDir(projectName), relativePath)
        return if (target.exists() && target.isFile) target.readText() else null
    }

    /** Borra un archivo o carpeta real del proyecto. */
    fun delete(projectName: String, relativePath: String): Boolean {
        val target = File(projectDir(projectName), relativePath)
        return target.deleteRecursively()
    }

    /** Borra el proyecto completo del almacenamiento real (ej. al borrar el proyecto). */
    fun deleteProject(projectName: String): Boolean {
        return projectDir(projectName).deleteRecursively()
    }

    /** Renombra la carpeta real del proyecto si el usuario le cambia el nombre. */
    fun renameProject(oldName: String, newName: String): Boolean {
        val oldDir = projectDir(oldName)
        val newDir = File(projectsRoot(), sanitizeName(newName))
        return oldDir.renameTo(newDir)
    }

    // ---------------- Permisos ----------------

    /** true si la app ya tiene acceso a todos los archivos (Android 11+). */
    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true // En Android 10 y anteriores, WRITE_EXTERNAL_STORAGE normal alcanza
        }
    }

    /**
     * Manda al usuario a la pantalla de Ajustes donde puede conceder
     * "Acceso a todos los archivos" para esta app. No hay diálogo in-app
     * para este permiso específico -- Android obliga a pasar por Ajustes.
     */
    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }
}
