package com.cocido.morfipolo.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseMigrations {
    /**
     * Migración de versión 1 a 2:
     * - Cambio de IDs de Long a String en todas las tablas
     * - Cambio de estado de enum a String
     * 
     * Como los tipos de clave primaria cambiaron, necesitamos recrear las tablas
     */
    val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Verificar si la tabla users existe antes de migrar
            val usersExists = try {
                database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='users'").use { cursor ->
                    cursor.count > 0
                }
            } catch (e: Exception) {
                false
            }
            
            if (usersExists) {
                // Recrear tabla users con id String
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        dni TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        email TEXT,
                        passwordHash TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
                
                // Copiar datos de users a users_new (si existen)
                try {
                    database.execSQL("""
                        INSERT INTO users_new (id, dni, nombre, email, passwordHash)
                        SELECT CAST(id AS TEXT), dni, nombre, email, COALESCE(passwordHash, '')
                        FROM users
                    """.trimIndent())
                } catch (e: Exception) {
                    // Si falla la copia, continuar sin datos
                }
                
                // Eliminar tabla antigua
                database.execSQL("DROP TABLE IF EXISTS users")
                
                // Renombrar tabla nueva
                database.execSQL("ALTER TABLE users_new RENAME TO users")
            } else {
                // Si no existe, crear la tabla directamente
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS users (
                        id TEXT NOT NULL PRIMARY KEY,
                        dni TEXT NOT NULL,
                        nombre TEXT NOT NULL,
                        email TEXT,
                        passwordHash TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent())
            }
            
            // Verificar si la tabla menus existe antes de migrar
            val menusExists = try {
                database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='menus'").use { cursor ->
                    cursor.count > 0
                }
            } catch (e: Exception) {
                false
            }
            
            if (menusExists) {
                // Recrear tabla menus con id String
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS menus_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        fecha INTEGER NOT NULL,
                        descripcion TEXT NOT NULL,
                        horarioInicio TEXT NOT NULL DEFAULT '08:00',
                        horarioFin TEXT NOT NULL DEFAULT '11:00',
                        estado TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Copiar datos de menus a menus_new (si existen)
                try {
                    database.execSQL("""
                        INSERT INTO menus_new (id, fecha, descripcion, horarioInicio, horarioFin, estado)
                        SELECT CAST(id AS TEXT), fecha, descripcion, 
                               COALESCE(horarioInicio, '08:00'), 
                               COALESCE(horarioFin, '11:00'),
                               CASE 
                                   WHEN estado = 0 THEN 'draft'
                                   WHEN estado = 1 THEN 'open'
                                   WHEN estado = 2 THEN 'closed'
                                   ELSE 'draft'
                               END
                        FROM menus
                    """.trimIndent())
                } catch (e: Exception) {
                    // Si falla la copia, continuar sin datos
                }
                
                // Eliminar tabla antigua
                database.execSQL("DROP TABLE IF EXISTS menus")
                
                // Renombrar tabla nueva
                database.execSQL("ALTER TABLE menus_new RENAME TO menus")
            } else {
                // Si no existe, crear la tabla directamente
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS menus (
                        id TEXT NOT NULL PRIMARY KEY,
                        fecha INTEGER NOT NULL,
                        descripcion TEXT NOT NULL,
                        horarioInicio TEXT NOT NULL DEFAULT '08:00',
                        horarioFin TEXT NOT NULL DEFAULT '11:00',
                        estado TEXT NOT NULL
                    )
                """.trimIndent())
            }
            
            // Verificar si la tabla menu_selections existe antes de migrar
            val menuSelectionsExists = try {
                database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='menu_selections'").use { cursor ->
                    cursor.count > 0
                }
            } catch (e: Exception) {
                false
            }
            
            if (menuSelectionsExists) {
                // Recrear tabla menu_selections con ids String
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS menu_selections_new (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        menuId TEXT NOT NULL,
                        fechaSeleccion INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(menuId) REFERENCES menus(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Crear índices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_menu_selections_userId ON menu_selections_new(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_menu_selections_menuId ON menu_selections_new(menuId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_menu_selections_userId_menuId ON menu_selections_new(userId, menuId)")
                
                // Copiar datos de menu_selections a menu_selections_new (si existen)
                try {
                    database.execSQL("""
                        INSERT INTO menu_selections_new (id, userId, menuId, fechaSeleccion)
                        SELECT CAST(id AS TEXT), CAST(userId AS TEXT), CAST(menuId AS TEXT), fechaSeleccion
                        FROM menu_selections
                    """.trimIndent())
                } catch (e: Exception) {
                    // Si falla la copia, continuar sin datos
                }
                
                // Eliminar tabla antigua
                database.execSQL("DROP TABLE IF EXISTS menu_selections")
                
                // Renombrar tabla nueva
                database.execSQL("ALTER TABLE menu_selections_new RENAME TO menu_selections")
            } else {
                // Si no existe, crear la tabla directamente
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS menu_selections (
                        id TEXT NOT NULL PRIMARY KEY,
                        userId TEXT NOT NULL,
                        menuId TEXT NOT NULL,
                        fechaSeleccion INTEGER NOT NULL,
                        FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE,
                        FOREIGN KEY(menuId) REFERENCES menus(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Crear índices
                database.execSQL("CREATE INDEX IF NOT EXISTS index_menu_selections_userId ON menu_selections(userId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_menu_selections_menuId ON menu_selections(menuId)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_menu_selections_userId_menuId ON menu_selections(userId, menuId)")
            }
        }
    }
}

