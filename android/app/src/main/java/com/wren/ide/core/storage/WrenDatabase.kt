package com.wren.ide.core.storage

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ProjectEntity::class, FileEntity::class],
    version = 1,
    exportSchema = false
)
abstract class WrenDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao
    abstract fun fileDao(): FileDao

    /** Repositorio que mantiene Room y el almacenamiento real sincronizados --
     * úsalo en vez de fileDao()/projectDao() directamente al crear/editar/borrar
     * archivos, para que también queden reflejados en
     * /storage/emulated/0/wren/wren_internal/wren/wren-proyect/<proyecto>/. */
    fun projectFileRepository(): ProjectFileRepository =
        ProjectFileRepository(fileDao(), projectDao())

    companion object {
        @Volatile
        private var INSTANCE: WrenDatabase? = null

        fun getDatabase(context: Context): WrenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WrenDatabase::class.java,
                    "wren_local_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
