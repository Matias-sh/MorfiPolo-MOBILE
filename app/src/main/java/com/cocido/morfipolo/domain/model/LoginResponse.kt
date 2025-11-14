package com.cocido.morfipolo.domain.model

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: User
)





