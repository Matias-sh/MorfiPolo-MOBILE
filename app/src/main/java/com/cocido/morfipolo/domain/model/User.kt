package com.cocido.morfipolo.domain.model

data class User(
    val id: String,
    val name: String,
    val lastName: String,
    val email: String,
    val dni: String,
    val birthDate: String,
    val isActive: Boolean,
    val roles: List<String>,
    val createdAt: String,
    val updatedAt: String,
    // El backend manda "dependency" como objeto, no el string "dependence" que
    // había acá antes. Ese desfasaje de nombre + tipo rompía el parseo de Moshi
    // en TODO login real (JsonDataException silenciada como error genérico).
    // Nullable con default: si el backend algún día lo omite, no vuelve a romper.
    val dependency: Dependency? = null
)

data class Dependency(
    val id: String,
    val name: String,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String
)
