package com.cocido.morfipolo.domain.model

data class ChangePasswordRequest(
    val password: String,
    val newPassword: String
)

