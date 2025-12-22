package com.cocido.morfipolo.data.repository

import com.cocido.morfipolo.data.remote.SessionExpiredException
import com.cocido.morfipolo.data.remote.api.MorfiPoloApiService
import com.cocido.morfipolo.domain.model.CreateVoteRequest
import com.cocido.morfipolo.domain.model.Vote
import com.cocido.morfipolo.domain.model.VotesResponse
import retrofit2.HttpException
import java.io.IOException

class VoteRepository(
    private val apiService: MorfiPoloApiService
) {
    
    suspend fun createVote(optionId: String, menuId: String): Result<Vote> {
        return try {
            val request = CreateVoteRequest(optionId, menuId)
            val response = apiService.createVote(request)
            
            if (response.isSuccessful) {
                val vote = response.body()
                if (vote != null) {
                    Result.success(vote)
                } else {
                    Result.failure(Exception("No se pudo registrar tu elección. Intenta de nuevo."))
                }
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Verificar diferentes tipos de errores 400
                        when {
                            errorBody.contains("already voted", ignoreCase = true) -> {
                                "Ya tienes un voto registrado para este menú"
                            }
                            else -> {
                                // Cualquier otro error 400 es probablemente por horario cerrado
                                "No se puede votar. Solo puedes elegir tu opción de 08:00 a 11:00."
                            }
                        }
                    }
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    404 -> "No se encontró la opción o el menú seleccionado"
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo registrar tu elección. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo registrar tu elección. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo registrar tu elección. Intenta de nuevo."))
        }
    }
    
    suspend fun deleteVote(voteId: String): Result<Boolean> {
        return try {
            val response = apiService.deleteVote(voteId)
            
            if (response.isSuccessful) {
                Result.success(true)
            } else {
                val errorBody = response.errorBody()?.string() ?: ""
                val errorMessage = when (response.code()) {
                    400 -> {
                        // Cualquier error 400 al eliminar voto es probablemente por horario cerrado
                        "No se puede eliminar el voto. Solo puedes modificar tu elección de 08:00 a 11:00."
                    }
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    404 -> "No se encontró el voto"
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo quitar tu elección. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo quitar tu elección. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo quitar tu elección. Intenta de nuevo."))
        }
    }
    
    /**
     * Obtiene todos los votos del usuario desde el servidor.
     * Nota: Aunque el nombre sugiere "hoy", el servidor puede devolver todos los votos del usuario.
     * Se mantiene el nombre por compatibilidad, pero se recomienda usar getAllUserVotes().
     */
    suspend fun getVotesForToday(): Result<List<Vote>> {
        return getAllUserVotes()
    }
    
    /**
     * Obtiene todos los votos del usuario desde el servidor.
     * WORKAROUND: El endpoint /votes no filtra por usuario autenticado, así que:
     * 1. Intenta obtener votos con parámetro user_id (si el backend lo soporta)
     * 2. Si no funciona, obtiene múltiples páginas y filtra localmente por userId
     * 
     * @param userId ID del usuario para filtrar (opcional, se intenta usar como query param)
     * @param maxPagesToSearch Máximo de páginas a buscar (null = buscar todas, por defecto 3 para optimización)
     */
    suspend fun getAllUserVotes(userId: String? = null, maxPagesToSearch: Int? = 3): Result<List<Vote>> {
        return try {
            // Primero intentar con parámetro user_id (si el backend lo soporta)
            val response = if (userId != null) {
                android.util.Log.d("VoteRepository", "Intentando obtener votos con user_id=$userId como parámetro de query...")
                apiService.getVotes(userId = userId, userIdAlt = userId)
            } else {
                apiService.getVotes()
            }
            
            if (response.isSuccessful) {
                val votesResponse = response.body()
                
                if (votesResponse != null) {
                    android.util.Log.d("VoteRepository", "Votos obtenidos del servidor: ${votesResponse.data.size} (página ${votesResponse.meta.currentPage} de ${votesResponse.meta.totalPages})")
                    
                    // Si el backend no filtró por usuario (todos los votos son de diferentes usuarios),
                    // necesitamos obtener más páginas y filtrar localmente
                    val allVotes = if (userId != null && votesResponse.data.isNotEmpty()) {
                        val userIdNormalized = userId.trim().lowercase()
                        val uniqueUserIds = votesResponse.data.map { it.user.id.trim().lowercase() }.distinct()
                        val hasUserVote = votesResponse.data.any { 
                            it.user.id.trim().lowercase() == userIdNormalized 
                        }
                        
                        android.util.Log.d("VoteRepository", "UserIds únicos en respuesta: ${uniqueUserIds.size}")
                        android.util.Log.d("VoteRepository", "¿Tiene voto el usuario en esta página? $hasUserVote")
                        
                        // Si hay más de un userId O no encontramos votos del usuario, el backend no filtró
                        // Necesitamos obtener más páginas
                        if (uniqueUserIds.size > 1 || !hasUserVote) {
                            android.util.Log.w("VoteRepository", "⚠️ El backend no filtró por usuario. Obteniendo múltiples páginas...")
                            getAllVotesPaginated(userId, votesResponse, maxPagesToSearch = maxPagesToSearch)
                        } else {
                            // El backend filtró correctamente o encontramos votos del usuario
                            android.util.Log.d("VoteRepository", "✅ Votos del usuario encontrados en primera página")
                            votesResponse.data.filter { 
                                it.user.id.trim().lowercase() == userIdNormalized 
                            }
                        }
                    } else {
                        // Si no proporcionamos userId, devolver todos los votos de la primera página
                        votesResponse.data
                    }
                    
                    // Log detallado de los primeros votos para debugging
                    allVotes.take(5).forEachIndexed { index, vote ->
                        android.util.Log.d("VoteRepository", "  Voto $index: ID=${vote.id}, Menú=${vote.menu.id} (${vote.menu.date}), Usuario=${vote.user.id}, Opción=${vote.option.name}")
                    }
                    
                    Result.success(allVotes)
                } else {
                    Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
                }
            } else {
                val errorMessage = when (response.code()) {
                    401 -> throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
                    500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                    else -> "No se pudo cargar la información. Intenta de nuevo."
                }
                Result.failure(Exception(errorMessage))
            }
        } catch (e: SessionExpiredException) {
            // Propagar la excepción de sesión expirada
            throw e
        } catch (e: com.squareup.moshi.JsonDataException) {
            Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
        } catch (e: retrofit2.HttpException) {
            if (e.code() == 401) {
                throw SessionExpiredException("Sesión expirada. Por favor, inicia sesión nuevamente.")
            }
            val errorMessage = when (e.code()) {
                500, 502, 503, 504 -> "El servidor no está disponible en este momento. Por favor, intenta más tarde."
                else -> "No se pudo cargar la información. Verifica tu conexión e intenta de nuevo."
            }
            Result.failure(Exception(errorMessage))
        } catch (e: java.io.IOException) {
            Result.failure(Exception("No se pudo conectar al servidor. Verifica tu conexión a internet."))
        } catch (e: Exception) {
            Result.failure(Exception("No se pudo cargar la información. Intenta de nuevo."))
        }
    }
    
    /**
     * Obtiene múltiples páginas de votos y filtra por userId localmente.
     * 
     * @param userId ID del usuario para filtrar
     * @param firstPageResponse Respuesta de la primera página
     * @param maxPagesToSearch Máximo de páginas a buscar (null = buscar todas)
     * @return Lista de votos del usuario, obtenidos de múltiples páginas
     */
    private suspend fun getAllVotesPaginated(userId: String, firstPageResponse: VotesResponse, maxPagesToSearch: Int? = null): List<Vote> {
        val allUserVotes = mutableListOf<Vote>()
        val totalPages = firstPageResponse.meta.totalPages
        val userIdNormalized = userId.trim().lowercase()
        
        // Agregar votos de la primera página que ya tenemos
        val firstPageUserVotes = firstPageResponse.data.filter { 
            it.user.id.trim().lowercase() == userIdNormalized 
        }
        allUserVotes.addAll(firstPageUserVotes)
        android.util.Log.d("VoteRepository", "Página 1: ${firstPageUserVotes.size} votos del usuario de ${firstPageResponse.data.size} totales")
        
        // Si maxPagesToSearch es null, buscar todas las páginas necesarias
        // Si está definido, limitar la búsqueda
        val maxPages = maxPagesToSearch ?: totalPages
        var currentPage = 2
        
        android.util.Log.d("VoteRepository", "Buscando votos del usuario en páginas 2-${minOf(maxPages, totalPages)} (de $totalPages totales)...")
        
        // Obtener páginas adicionales
        while (currentPage <= totalPages && currentPage <= maxPages) {
            try {
                val response = apiService.getVotes(page = currentPage, limit = 20)
                if (response.isSuccessful) {
                    val pageResponse = response.body()
                    if (pageResponse != null) {
                        val pageUserVotes = pageResponse.data.filter { 
                            it.user.id.trim().lowercase() == userIdNormalized
                        }
                        
                        if (pageUserVotes.isNotEmpty()) {
                            allUserVotes.addAll(pageUserVotes)
                            android.util.Log.d("VoteRepository", "Página $currentPage: ${pageUserVotes.size} votos del usuario de ${pageResponse.data.size} totales")
                        }
                    }
                }
                // Delay mínimo para no sobrecargar el servidor
                kotlinx.coroutines.delay(30)
                currentPage++
            } catch (e: Exception) {
                android.util.Log.w("VoteRepository", "Error obteniendo página $currentPage: ${e.message}")
                currentPage++
            }
        }
        
        android.util.Log.d("VoteRepository", "✅ Total votos del usuario obtenidos: ${allUserVotes.size} (buscado hasta página ${currentPage - 1} de $totalPages)")
        return allUserVotes
    }
    
    /**
     * Obtiene el voto del usuario para un menú específico.
     * OPTIMIZACIÓN: Usa el parámetro menu_id en la query para obtener solo el voto necesario.
     * 
     * @param menuId ID del menú
     * @param userId ID del usuario
     * @param maxPagesToSearch Máximo de páginas a buscar (por defecto null = todas las páginas para encontrar votos antiguos)
     * @return El voto del usuario para el menú especificado, o null si no existe
     */
    suspend fun getUserVoteForMenu(menuId: String, userId: String, maxPagesToSearch: Int? = null): Vote? {
        return try {
            android.util.Log.d("VoteRepository", "Buscando voto para menú: $menuId, usuario: $userId")
            
            // OPTIMIZACIÓN: Intentar primero con menu_id en la query (mucho más rápido)
            try {
                val response = apiService.getVotes(menuId = menuId, userId = userId, userIdAlt = userId)
                if (response.isSuccessful) {
                    val votesResponse = response.body()
                    if (votesResponse != null && votesResponse.data.isNotEmpty()) {
                        // Filtrar por userId (por si el backend no filtra correctamente)
                        val userIdNormalized = userId.trim().lowercase()
                        val vote = votesResponse.data.find { 
                            it.menu.id == menuId && it.user.id.trim().lowercase() == userIdNormalized
                        }
                        if (vote != null) {
                            android.util.Log.d("VoteRepository", "✅ Voto encontrado con query optimizada: ${vote.id} para opción: ${vote.option.id} (${vote.option.name})")
                            return vote
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VoteRepository", "Query optimizada falló, usando método alternativo: ${e.message}")
            }
            
            // Fallback: Si la query optimizada no funciona, buscar en todos los votos del usuario
            // Para búsquedas individuales de menús específicos, buscar en más páginas si es necesario
            android.util.Log.d("VoteRepository", "Usando método alternativo (búsqueda extendida para menú específico)...")
            
            // Para búsquedas individuales, buscar en más páginas para encontrar votos antiguos
            try {
                val response = apiService.getVotes(userId = userId, userIdAlt = userId)
                if (response.isSuccessful) {
                    val votesResponse = response.body()
                    if (votesResponse != null) {
                        // Si maxPagesToSearch es null, buscar en todas las páginas
                        // Si es un número específico, limitar a ese número
                        val pagesToSearch = maxPagesToSearch ?: votesResponse.meta.totalPages
                        
                        // Buscar en las páginas según el parámetro
                        val extendedVotes = getAllVotesPaginated(userId, votesResponse, maxPagesToSearch = pagesToSearch)
                        val vote = extendedVotes.find { 
                            it.menu.id == menuId && it.user.id.trim().lowercase() == userId.trim().lowercase()
                        }
                        if (vote != null) {
                            android.util.Log.d("VoteRepository", "✅ Voto encontrado: ${vote.id} para opción: ${vote.option.id} (${vote.option.name})")
                        } else {
                            android.util.Log.w("VoteRepository", "❌ No se encontró voto para menú: $menuId, usuario: $userId (buscado en ${pagesToSearch ?: votesResponse.meta.totalPages} de ${votesResponse.meta.totalPages} páginas)")
                        }
                        return vote
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("VoteRepository", "Error en búsqueda extendida: ${e.message}")
            }
            
            android.util.Log.w("VoteRepository", "❌ No se encontró voto para menú: $menuId, usuario: $userId")
            null
        } catch (e: Exception) {
            android.util.Log.e("VoteRepository", "Error al obtener voto para menú: $menuId", e)
            null
        }
    }
    
    suspend fun createVoteOrReplace(optionId: String, menuId: String, userId: String): Result<Vote> {
        return try {
            // OPTIMIZACIÓN: Para el menú del día (reciente), buscar solo en 3 páginas
            // Esto es mucho más rápido que buscar en todas las páginas
            val existingVote = getUserVoteForMenu(menuId, userId, maxPagesToSearch = 3)
            existingVote?.let {
                deleteVote(it.id)
            }
            
            // Crear nuevo voto
            createVote(optionId, menuId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

