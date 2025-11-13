package com.cocido.morfipolo.domain.model

data class User(
    val id: String,
    val name: String,
    val lastName: String,
    val email: String,
    val dni: String,
    val dependence: String,
    val birthDate: String,
    val isActive: Boolean,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String
)




