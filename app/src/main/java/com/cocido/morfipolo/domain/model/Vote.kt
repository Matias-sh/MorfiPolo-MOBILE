package com.cocido.morfipolo.domain.model

data class Vote(
    val id: String,
    val confirmed: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val user: User,
    val menu: Menu,
    val option: MenuOption
)



