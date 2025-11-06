package com.cocido.morfipolo.domain.model

import java.util.Date

enum class MenuStatus {
    ABIERTO,
    CERRADO
}

data class Menu(
    val id: Long = 0,
    val fecha: Date,
    val descripcion: String,
    val horarioInicio: String = "08:00",
    val horarioFin: String = "11:00",
    val estado: MenuStatus = MenuStatus.ABIERTO
)


