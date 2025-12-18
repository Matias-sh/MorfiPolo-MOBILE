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
     */
    suspend fun getAllUserVotes(userId: String? = null): Result<List<Vote>> {
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
                            getAllVotesPaginated(userId, votesResponse)
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
     * WORKAROUND: Necesario porque el backend no filtra por usuario autenticado.
     * 
     * @param userId ID del usuario para filtrar
     * @param firstPageResponse Respuesta de la primera página
     * @return Lista de votos del usuario, obtenidos de múltiples páginas
     */
    private suspend fun getAllVotesPaginated(userId: String, firstPageResponse: VotesResponse): List<Vote> {
        val allUserVotes = mutableListOf<Vote>()
        val totalPages = firstPageResponse.meta.totalPages
        val userIdNormalized = userId.trim().lowercase()
        
        // Agregar votos de la primera página que ya tenemos
        val firstPageUserVotes = firstPageResponse.data.filter { 
            it.user.id.trim().lowercase() == userIdNormalized 
        }
        allUserVotes.addAll(firstPageUserVotes)
        android.util.Log.d("VoteRepository", "Página 1: ${firstPageUserVotes.size} votos del usuario de ${firstPageResponse.data.size} totales")
        
        // Buscar en TODAS las páginas necesarias para encontrar todos los votos del usuario
        // Estrategia: buscar hasta que no encontremos votos en varias páginas consecutivas
        val maxConsecutiveEmpty = 5 // Parar después de 5 páginas consecutivas sin votos
        var consecutiveEmptyPages = 0
        var currentPage = 2
        
        android.util.Log.d("VoteRepository", "Buscando votos del usuario en todas las páginas (total: $totalPages)...")
        
        // Obtener páginas adicionales hasta encontrar todos los votos
        while (currentPage <= totalPages && consecutiveEmptyPages < maxConsecutiveEmpty) {
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
                            consecutiveEmptyPages = 0 // Resetear contador
                            android.util.Log.d("VoteRepository", "Página $currentPage: ${pageUserVotes.size} votos del usuario de ${pageResponse.data.size} totales")
                            android.util.Log.d("VoteRepository", "✅ Encontrados ${pageUserVotes.size} votos del usuario en página $currentPage")
                        } else {
                            consecutiveEmptyPages++
                            android.util.Log.d("VoteRepository", "Página $currentPage: 0 votos del usuario (consecutivos vacíos: $consecutiveEmptyPages)")
                            
                            // Si no encontramos votos en varias páginas consecutivas, probablemente ya encontramos todos
                            if (consecutiveEmptyPages >= maxConsecutiveEmpty) {
                                android.util.Log.d("VoteRepository", "⏹️ Parando búsqueda: ${consecutiveEmptyPages} páginas consecutivas sin votos del usuario")
                                break
                            }
                        }
                    }
                }
                // Pequeño delay para no sobrecargar el servidor
                kotlinx.coroutines.delay(50) // Reducido para ser más rápido
                currentPage++
            } catch (e: Exception) {
                android.util.Log.w("VoteRepository", "Error obteniendo página $currentPage: ${e.message}")
                consecutiveEmptyPages++
                currentPage++
                // Continuar con la siguiente página
                if (consecutiveEmptyPages >= maxConsecutiveEmpty) {
                    break
                }
            }
        }
        
        android.util.Log.d("VoteRepository", "✅ Total votos del usuario obtenidos: ${allUserVotes.size} (buscado hasta página ${currentPage - 1} de $totalPages)")
        return allUserVotes
    }
    
    /**
     * Obtiene el voto del usuario para un menú específico.
     * Este método busca en todos los votos del usuario, no solo los de hoy.
     * 
     * @param menuId ID del menú
     * @param userId ID del usuario
     * @return El voto del usuario para el menú especificado, o null si no existe
     */
    suspend fun getUserVoteForMenu(menuId: String, userId: String): Vote? {
        return try {
            android.util.Log.d("VoteRepository", "Buscando voto para menú: $menuId, usuario: $userId")
            val votesResult = getAllUserVotes(userId)
            val votes = votesResult.getOrNull() ?: emptyList()
            android.util.Log.d("VoteRepository", "Buscando en ${votes.size} votos: menú=$menuId, usuario=$userId")
            val vote = votes.find { vote ->
                val menuMatch = vote.menu.id == menuId
                val userMatch = vote.user.id == userId
                android.util.Log.d("VoteRepository", "  Comparando: menú ${vote.menu.id} == $menuId? $menuMatch, usuario ${vote.user.id} == $userId? $userMatch")
                menuMatch && userMatch
            }
            if (vote != null) {
                android.util.Log.d("VoteRepository", "✅ Voto encontrado: ${vote.id} para opción: ${vote.option.id} (${vote.option.name})")
            } else {
                android.util.Log.w("VoteRepository", "❌ No se encontró voto para menú: $menuId, usuario: $userId")
                android.util.Log.d("VoteRepository", "   Votos disponibles: ${votes.map { "${it.menu.id} (${it.menu.date})" }.joinToString(", ")}")
            }
            vote
        } catch (e: Exception) {
            android.util.Log.e("VoteRepository", "Error al obtener voto para menú: $menuId", e)
            null
        }
    }
    
    suspend fun createVoteOrReplace(optionId: String, menuId: String, userId: String): Result<Vote> {
        return try {
            // Primero verificar si ya existe un voto y eliminarlo
            val existingVote = getUserVoteForMenu(menuId, userId)
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

