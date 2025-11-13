package com.cocido.morfipolo.domain.model

data class MenusResponse(
    val data: List<Menu>,
    val meta: Meta,
    val links: Links
)

data class Meta(
    val itemsPerPage: Int,
    val totalItems: Int,
    val currentPage: Int,
    val totalPages: Int,
    val sortBy: List<List<String>>
)

data class Links(
    val current: String
)



