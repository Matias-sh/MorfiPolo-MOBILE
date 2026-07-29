package com.cocido.morfipolo.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey
    val id: String,
    val fecha: Long, // Timestamp
    val descripcion: String,
    val horarioInicio: String = "08:00",
    val horarioFin: String = "11:00",
    val estado: String, // "open", "closed", "draft"
    // Opciones de voto serializadas como JSON (lista de MenuOption). Antes no se
    // guardaban, así que cualquier fallback a caché offline mostraba un menú sin
    // opciones para elegir (indistinguible de "no hay menú hoy").
    val opcionesJson: String? = null
)




