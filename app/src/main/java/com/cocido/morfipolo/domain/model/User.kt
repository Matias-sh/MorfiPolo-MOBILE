package com.cocido.morfipolo.domain.model

import com.squareup.moshi.Json

data class User(
    val id: String = "",
    val name: String = "",
    val lastName: String = "",
    val email: String = "",
    val dni: String = "",
    @Json(name = "dependence")
    val dependence: String = "",
    @Json(name = "dependency")
    val dependency: Dependency? = null,
    val birthDate: String = "",
    val isActive: Boolean = true,
    val roles: List<String> = emptyList(),
    val createdAt: String = "",
    val updatedAt: String = ""
) {
    val dependencyName: String
        get() = dependency?.name ?: dependence
}

data class Dependency(
    val id: String = "",
    val name: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val isActive: Boolean = true
)




