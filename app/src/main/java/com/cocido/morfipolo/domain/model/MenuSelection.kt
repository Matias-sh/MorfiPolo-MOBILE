package com.cocido.morfipolo.domain.model

import java.util.Date

data class MenuSelection(
    val id: Long = 0,
    val userId: Long,
    val menuId: Long,
    val fechaSeleccion: Date = Date()
)










