package com.cocido.morfipolo.domain.model

data class User(
    val id: Long = 0,
    val dni: String,
    val nombre: String,
    val email: String? = null,
    val passwordHash: String
)


