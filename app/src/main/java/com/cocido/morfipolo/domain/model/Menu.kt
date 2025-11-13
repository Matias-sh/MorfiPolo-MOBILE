package com.cocido.morfipolo.domain.model

data class Menu(
    val id: String,
    val date: String, // formato ISO "YYYY-MM-DD"
    val description: String,
    val start_time: String, // formato ISO 8601
    val end_time: String, // formato ISO 8601
    val status: String, // "open", "closed", "draft"
    val created_at: String,
    val updated_at: String,
    val options: List<MenuOption>? = null // Opcional, puede no venir en respuestas de votos
) {
    // Helper para obtener options de forma segura (sin null)
    fun getOptionsOrEmpty(): List<MenuOption> = options ?: emptyList()
}




