package com.cocido.morfipolo.data.local.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.cocido.morfipolo.domain.model.MenuStatus

@Entity(tableName = "menus")
data class MenuEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fecha: Long, // Timestamp
    val descripcion: String,
    val horarioInicio: String = "08:00",
    val horarioFin: String = "11:00",
    val estado: MenuStatus = MenuStatus.ABIERTO
)


